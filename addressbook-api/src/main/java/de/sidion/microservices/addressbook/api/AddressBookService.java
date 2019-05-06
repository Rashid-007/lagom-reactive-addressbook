package de.sidion.microservices.addressbook.api;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.pathCall;
import static com.lightbend.lagom.javadsl.api.Service.restCall;
import static com.lightbend.lagom.javadsl.api.Service.topic;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.Method;
import de.sidion.microservices.addressbook.pagination.PaginatedSequence;

import java.util.Optional;
import java.util.UUID;

/**
 * The address book service interface.
 * <p>
 * This describes everything that Lagom needs to know about how to serve and
 * consume the address book.
 */
public interface AddressBookService extends Service {

  /**
   * get the list of contacts in paginated format.
   * @return PaginatedSequence<ContactSummary>
   */
  ServiceCall<NotUsed, PaginatedSequence<ContactSummary>> getAllContacts(Optional<Integer> pageNo, Optional<Integer> pageSize);

  /**
   * Creates a contact using ContactData
   * @return
   */
  ServiceCall<ContactData, Contact> createContact();

  /**
   * Update a contact using its id
   * @param id The id of the contact
   * @return Contact
   */
  ServiceCall<ContactData, Contact> updateContact(UUID id);

  /**
   * Get a specific contact using it id
   * @param id The id of the contact
   * @return
   */
  ServiceCall<NotUsed, Contact> getContact(UUID id);

  @Override
  default Descriptor descriptor() {
    // @formatter:off
    return named("addressbook").withCalls(
        restCall(Method.GET,"/api/contact?pageNo&pageSize",  this::getAllContacts),
        restCall(Method.POST,"/api/contact",  this::createContact),
        restCall(Method.GET, "/api/contact/:id",  this::getContact),
        restCall(Method.PUT, "/api/contact/:id", this::updateContact)
        //restCall(Method.DELETE, "/api/contact/:id", this::deleteContact),
      ).withAutoAcl(true);
    // @formatter:on
  }


}
