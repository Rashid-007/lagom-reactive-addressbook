package de.sidion.microservices.addressbook.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.lightbend.lagom.serialization.Jsonable;
import de.sidion.microservices.addressbook.api.Email;
import de.sidion.microservices.addressbook.api.PhoneNumber;
import lombok.Data;

import java.util.List;

@Data
public class PContactData implements Jsonable {
    String firstName;
    String lastName;
    List<PhoneNumber> phoneNumberList;
    List<Email> emailList;

    @JsonCreator
// Tells Jackson use this constructor to to create Object from JSON since there is no setters for immutable object
    public PContactData(String firstName, String lastName, List<PhoneNumber> phoneNumberList, List<Email> emailList) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumberList = phoneNumberList;
        this.emailList = emailList;
    }
}
