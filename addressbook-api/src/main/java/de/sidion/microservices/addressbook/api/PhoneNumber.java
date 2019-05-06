package de.sidion.microservices.addressbook.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Value;

import java.util.UUID;

@Value
public class PhoneNumber {
    UUID id;
    String label;
    String number;

    @JsonCreator
    public PhoneNumber(String label, String number, UUID id){
        this.id = id;
        this.label = label;
        this.number = number;
    }
}
