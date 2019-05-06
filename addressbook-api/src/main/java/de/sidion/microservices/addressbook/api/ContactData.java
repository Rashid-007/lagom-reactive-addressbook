package de.sidion.microservices.addressbook.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.List;

@Value
@Wither
public class ContactData {
    String firstName;
    String lastName;
    List<PhoneNumber> phoneNumberList;
    List<Email> emailList;

    @JsonCreator// Tells Jackson use this constructor to to create Object from JSON since there is no setters for immutable object
    public ContactData(String firstName, String lastName, List<PhoneNumber> phoneNumberList, List<Email> emailList) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumberList = phoneNumberList;
        this.emailList = emailList;
    }
}
