package de.sidion.microservices.addressbook.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Value;

import java.util.UUID;

@Value
public class Email {
    UUID id;
    String label;
    String address;

    @JsonCreator
    public Email(String label, String address, UUID id){
        this.id = id;
        this.label = label;
        this.address = address;
    }
}
