package de.sidion.microservices.addressbook.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class Contact {
    UUID id;
    ContactData contactData;

    @JsonCreator
    public Contact(UUID id, ContactData contactData) {
        this.contactData = contactData;
        this.id = id;
    }
}
