package de.sidion.microservices.addressbook.impl;

import com.datastax.driver.core.utils.UUIDs;
import de.sidion.microservices.addressbook.api.Contact;
import de.sidion.microservices.addressbook.api.ContactData;
import de.sidion.microservices.addressbook.api.Email;
import de.sidion.microservices.addressbook.api.PhoneNumber;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 *
 */
public class Mappers {

    public static Contact toApi(PContact contact) {
        ContactData data = toApi(contact.getPContactData());
        return new Contact(contact.getId(), data);
    }

    public static ContactData toApi(PContactData details) {
        return new ContactData(
                details.getFirstName(),
                details.getLastName(),
                details.getPhoneNumberList(),
                details.getEmailList());
    }

    public static PContactData fromApi(ContactData data) {
        return new PContactData(
                data.getFirstName(),
                data.getLastName(),
                getPhoneNumberListWithSystemIds(data.getPhoneNumberList()),
                getEmailListWithSystemIds(data.getEmailList())
                );
    }
    private static List<PhoneNumber> getPhoneNumberListWithSystemIds(List<PhoneNumber> list){
        return list.stream().map(n -> {
            UUID id = UUIDs.timeBased();
            PhoneNumber number = new PhoneNumber(n.getLabel(), n.getNumber(), id);
            return number;
        }).collect(Collectors.toList());
    }

    private static List<Email> getEmailListWithSystemIds(List<Email> list){
        return list.stream().map(n -> {
            UUID id = UUIDs.timeBased();
            Email email = new Email(n.getLabel(), n.getAddress(), id);
            return email;
        }).collect(Collectors.toList());
    }
}
