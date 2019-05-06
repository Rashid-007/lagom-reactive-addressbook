package de.sidion.microservices.addressbook.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Value;

import java.util.UUID;

@Value
public class PContact {
    UUID id;
    PContactData pContactData;

    @JsonCreator
    public PContact(UUID id, PContactData pContactData) {

        this.id = id;
        this.pContactData = pContactData;
    }

    /**
     * Returns a copy of this instance with updates on the publicly editable fields.
     */
    public PContact withDetails(PContactData updateData) {
        return new PContact(id, updateData);
    }
}
