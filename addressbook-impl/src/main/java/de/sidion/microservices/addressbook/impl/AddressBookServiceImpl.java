package de.sidion.microservices.addressbook.impl;

import akka.NotUsed;
import com.datastax.driver.core.utils.UUIDs;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.deser.ExceptionMessage;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.api.transport.TransportException;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRef;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;

import javax.inject.Inject;

import de.sidion.microservices.addressbook.api.*;
import de.sidion.microservices.addressbook.pagination.PaginatedSequence;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the AddressBookService.
 */
public class AddressBookServiceImpl implements AddressBookService {

  private static final Integer DEFAULT_PAGE_SIZE = 10;

  private final PersistentEntityRegistry persistentEntityRegistry;
  private final ContactRepository contactRepository;

  @Inject
  public AddressBookServiceImpl(PersistentEntityRegistry persistentEntityRegistry, ContactRepository contactRepository) {

    this.persistentEntityRegistry = persistentEntityRegistry;
    this.contactRepository = contactRepository;
    persistentEntityRegistry.register(PContactEntity.class);
  }


  @Override
  public ServiceCall<NotUsed, PaginatedSequence<ContactSummary>> getAllContacts(Optional<Integer> pageNo, Optional<Integer> pageSize) {

    return req -> contactRepository.getContatcts(pageNo.orElse(0), pageSize.orElse(DEFAULT_PAGE_SIZE));
  }

  @Override
  public ServiceCall<ContactData, Contact> createContact() {

    return contactData -> {
      UUID contactId = UUIDs.timeBased();
      PContactData details = Mappers.fromApi(contactData);

      PContact pContact = new PContact(contactId, details);
      return entityRef(contactId).ask(new PContactCommand.CreateContact(pContact)).thenApply(done -> Mappers.toApi(pContact));
    };
  }

  @Override
  public ServiceCall<ContactData, Contact> updateContact(UUID contactId) {

      return contactData -> {
          PContactData details = Mappers.fromApi(contactData);
          PContactCommand.UpdateContact updateContact = new PContactCommand.UpdateContact(details);
          return entityRef(contactId)
                  .ask(updateContact)
                  .handle((pContact, updateException) -> {
                              if (updateException != null) {
                                      throw new TransportException(TransportErrorCode.fromHttp(409),
                                              new ExceptionMessage("UpdateFailed", updateException.getMessage()));
                              } else {
                                  return Mappers.toApi((PContact) pContact);
                              }
                          }
                  );
      };
  }

  @Override
  public ServiceCall<NotUsed, Contact> getContact(UUID id) {
    return req -> entityRef(id).ask(PContactCommand.GetContact.INSTANCE).thenApply(maybeContact -> {
      if (maybeContact.isPresent()) {
        return Mappers.toApi(maybeContact.get());
      } else {
        throw new NotFound("Contact " + id + " not found");
      }
    });
  }

  private PersistentEntityRef<PContactCommand> entityRef(UUID contactId) {
    return persistentEntityRegistry.refFor(PContactEntity.class, contactId.toString());
  }
}
