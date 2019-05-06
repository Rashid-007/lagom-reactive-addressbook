package de.sidion.microservices.addressbook.api;

import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * The most important fields of contact. We don't want to completely show the domain model of contact.
 */
//@Value
@Getter
public class ContactSummary {
    UUID id;
    String firstName;
    String lastName;

    List<PhoneNumber> phoneNumbers;
    List<Email> emails;

    public ContactSummary(UUID id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumbers = phoneNumbers;
        this.emails = emails;
    }

    public ContactSummary setEmailList(List<Email> emails){
        this.emails = emails;
        return this;
    }

    public ContactSummary setPhoneNumberList(List<PhoneNumber> phoneNumbers){
        this.phoneNumbers = phoneNumbers;
        return this;
    }
}
