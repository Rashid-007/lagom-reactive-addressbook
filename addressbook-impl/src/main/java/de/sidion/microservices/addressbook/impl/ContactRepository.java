package de.sidion.microservices.addressbook.impl;

import akka.Done;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.ReadSide;
import com.lightbend.lagom.javadsl.persistence.ReadSideProcessor;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;
import de.sidion.microservices.addressbook.api.ContactSummary;
import de.sidion.microservices.addressbook.api.Email;
import de.sidion.microservices.addressbook.api.PhoneNumber;
import de.sidion.microservices.addressbook.pagination.PaginatedSequence;
import org.pcollections.PSequence;
import org.pcollections.TreePVector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide.completedStatements;
import static de.sidion.microservices.addressbook.core.CompletionStageUtils.accept;
import static de.sidion.microservices.addressbook.core.CompletionStageUtils.doAll;

@Singleton
public class ContactRepository {

    private final CassandraSession session;

    @Inject
    public ContactRepository(CassandraSession session, ReadSide readSide){
        this.session = session;
        readSide.register(PContactEventProcessor.class);
    }
    CompletionStage<PaginatedSequence<CompletionStage<ContactSummary>>> getContatcts(int pageNo, int pageSize){
        //TODO Return all contact by select and join query operations on the three tables create for the journal
        return contactsCount()
                .thenCompose(count -> {
                    int offset = pageNo * pageSize;
                    int limit = (pageNo + 1) * pageSize;
                    CompletionStage<PSequence<CompletionStage<ContactSummary>>> items = offset > count ?
                            CompletableFuture.completedFuture(TreePVector.empty()) :
                            selectContacts(offset, limit);
                    return items.thenApply(seq -> new PaginatedSequence<>(seq, pageNo, pageSize, count));
                });
    }
    private CompletionStage<Integer> contactsCount(){
        return session.selectOne(
                "SELECT COUNT(*) FROM contact"
        ).thenApply(row -> (int) row.get().getLong("count"));
    }

    private CompletionStage<PSequence<CompletionStage<ContactSummary>>> selectContacts(int offset, int limit){
        return session
                .selectAll(
                        "SELECT * FROM contact LIMIT ?", limit
                )
                .thenApply(List::stream)
                .thenApply(rows -> rows.skip(offset))
                .thenApply(rows -> rows.map(ContactRepository::buildContactSummary))
                .thenApply(contactSummery -> contactSummery.collect(Collectors.toList()))
                //.thenApply(TreePVector::from)
                //removable
                .thenApply(contactSummaries ->
                        contactSummaries.stream().map(contact -> {
                                    CompletionStage<List<Email>> emailList = session.selectAll("SELECT * FROM email WHERE contact_id = ?", contact.getId())
                                            .thenApply(List::stream)
                                            .thenApply(email -> email.map(ContactRepository::buildEmail))
                                            .thenApply(emails -> emails.collect(Collectors.toList()));
                                    return emailList.thenApply(emails -> contact.setEmailList(emails));
                                }
                                        //.thenApply(emails -> contact.setEmailList(emails))
                        ).collect(Collectors.toList())



                )
                .thenApply(TreePVector::from);
    }

    private static ContactSummary buildContactSummary(Row row){
        return new ContactSummary(
                row.getUUID("contact_id"),
                row.getString("first_name"),
                row.getString("last_name")
        );
    }

    private static Email buildEmail(Row row){
        return new Email(row.getString("label"), row.getString("address"), row.getUUID("id"));
    }

/*    private static PhoneNumber buildPhoneNumber(Row row){
        UUID contact_id = row.getUUID("contact_id");
    }*/

    private static class PContactEventProcessor extends ReadSideProcessor<PContactEvent> {
        private CassandraSession session;
        private CassandraReadSide readSide;

        private PreparedStatement insertContactStatement;
        private PreparedStatement insertPhoneNumberStatement;
        private PreparedStatement insertEmailStatement;

        private PreparedStatement updateContactStatement;
        private PreparedStatement updatePhoneNumberStatement;
        private PreparedStatement updateEmailStatement;

        @Inject
        public PContactEventProcessor(CassandraSession session, CassandraReadSide readSide) {
            this.session = session;
            this.readSide = readSide;
        }

        @Override
        public ReadSideHandler<PContactEvent> buildHandler() {
            return readSide.<PContactEvent>builder("pContactEventOffset")
                    .setGlobalPrepare(this::createTables)
                    .setPrepare(tag -> prepareStatements())
                    .setEventHandler(PContactEvent.ContactCreated.class,
                            e -> insertContactData(e.getPContact()))
                    .setEventHandler(PContactEvent.ContactUpdated.class,
                            e -> updateContactData(e))
                    .build();
        }

        private CompletionStage<List<BoundStatement>> insertContactData(PContact pContact) {
            List<BoundStatement> statements = new ArrayList<>();
            statements.addAll(insertPhoneNumber(pContact));
            statements.addAll(insertEmail(pContact));
            statements.add(insertContact(pContact));

            return completedStatements(
                    statements
            );
        }

        private CompletionStage<List<BoundStatement>> updateContactData(PContactEvent.ContactUpdated contactUpdated) {
            List<BoundStatement> statements = new ArrayList<>();
            statements.addAll(updatePhoneNumber(contactUpdated));
            statements.addAll(updateEmail(contactUpdated));
            statements.add(updateContact(contactUpdated));
            return completedStatements(
                    statements
            );
        }

        private BoundStatement insertContact(PContact pContact) {
            return insertContactStatement.bind(
                    pContact.getId(),
                    pContact.getPContactData().getFirstName(),
                    pContact.getPContactData().getLastName()
            );
        }

        private List<BoundStatement> insertPhoneNumber(PContact pContact) {
            List<BoundStatement> statements = new ArrayList<>();
            pContact.getPContactData().getPhoneNumberList().forEach(n ->
                statements.add(insertPhoneNumberStatement.bind(
                        n.getId(),
                        n.getLabel(),
                        n.getNumber(),
                        pContact.getId()
                ))
            );
            return statements;
        }

        private List<BoundStatement> insertEmail(PContact pContact) {
            List<BoundStatement> statements = new ArrayList<>();
            pContact.getPContactData().getEmailList().forEach(e ->
                    statements.add(insertEmailStatement.bind(
                            e.getId(),
                            e.getLabel(),
                            e.getAddress(),
                            pContact.getId()
                    ))
            );
            return statements;
        }

        private BoundStatement updateContact(PContactEvent.ContactUpdated contactUpdated) {
            return updateContactStatement.bind(
                    contactUpdated.getContactDetails().getFirstName(),
                    contactUpdated.getContactDetails().getLastName(),
                    contactUpdated.getContactId()
            );
        }

        private List<BoundStatement> updatePhoneNumber(PContactEvent.ContactUpdated contactUpdated) {
            List<BoundStatement> statements = new ArrayList<>();
            contactUpdated.getContactDetails().getPhoneNumberList().forEach(n ->
                statements.add(
                        updatePhoneNumberStatement.bind(
                        n.getLabel(),
                        n.getNumber(),
                        contactUpdated.getContactId(),
                        n.getId()
                        )
            ));
            return statements;
        }

        private List<BoundStatement> updateEmail(PContactEvent.ContactUpdated contactUpdated) {
            List<BoundStatement> statements = new ArrayList<>();
            contactUpdated.getContactDetails().getEmailList().forEach(e ->
                    statements.add(
                            updateEmailStatement.bind(
                                    e.getLabel(),
                                    e.getAddress(),
                                    contactUpdated.getContactId(),
                                    e.getId()
                            )
                    ));
            return statements;
        }

        private CompletionStage<Done> createTables() {
            return doAll(
                    session.executeCreateTable(
                            "CREATE TABLE IF NOT EXISTS contact (contact_id timeuuid, first_name text, last_name text, PRIMARY KEY (contact_id))"
                    ),
                    session.executeCreateTable(
                            "CREATE TABLE IF NOT EXISTS phone_number (" +
                                    "id timeuuid, " +
                                    "label text, " +
                                    "number text, " +
                                    "contact_id timeuuid, " +
                                    "PRIMARY KEY (id, contact_id))"
                    ),
                    session.executeCreateTable(
                            "CREATE TABLE IF NOT EXISTS email (" +
                                    "id timeuuid, " +
                                    "label text, " +
                                    "address text, " +
                                    "contact_id timeuuid, " +
                                    "PRIMARY KEY (id, contact_id))"
                    )
            );
        }

        @Override
        public PSequence<AggregateEventTag<PContactEvent>> aggregateTags() {
            return PContactEvent.TAG.allTags();
        }


        private CompletionStage<Done> prepareStatements() {
            return doAll(
                    prepareInsertContactStatement(),
                    prepareInsertPhoneNumberStatement(),
                    prepareInsertEmailStatement(),
                    prepareUpdateContactStatement(),
                    prepareUpdatePhoneNumberStatement(),
                    prepareUpdateEmailStatement()

            );
        }

        private CompletionStage<Done> prepareInsertEmailStatement() {
            return session
                    .prepare("INSERT INTO email(id, label, address, contact_id) VALUES(?, ?, ?, ?)")
                    .thenApply(accept(s -> insertEmailStatement = s));
        }

        private CompletionStage<Done> prepareInsertPhoneNumberStatement() {
            return session
                    .prepare("INSERT INTO phone_number(id, label, number, contact_id) VALUES(?, ?, ?, ?)")
                    .thenApply(accept(s -> insertPhoneNumberStatement =s));
        }

        private CompletionStage<Done> prepareInsertContactStatement() {
            return session
                    .prepare("INSERT INTO contact(contact_id, first_name, last_name) VALUES(?, ?, ?)")
                    .thenApply(accept(s -> insertContactStatement = s));
        }

        private CompletionStage<Done> prepareUpdateEmailStatement(){
            return session
                    .prepare("UPDATE email SET label = ?, address = ? " +
                            "WHERE contact_id = ? AND id = ?")
                    .thenApply(accept(s -> updateEmailStatement = s));
        }

        private CompletionStage<Done> prepareUpdatePhoneNumberStatement(){
            return session
                    .prepare("UPDATE phone_number SET  label = ?, number = ? " +
                            "WHERE contact_id =? AND id = ?")
                    .thenApply(accept(s -> updatePhoneNumberStatement = s));
        }

        private CompletionStage<Done> prepareUpdateContactStatement(){
            return session
                    .prepare("UPDATE contact SET first_name = ?, last_name = ? " +
                            "WHERE contact_id = ?")
                    .thenApply(accept(s -> updateContactStatement = s));
        }
    }

}
