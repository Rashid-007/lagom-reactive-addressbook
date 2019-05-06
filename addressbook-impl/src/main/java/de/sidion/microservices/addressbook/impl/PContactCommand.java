package de.sidion.microservices.addressbook.impl;

import akka.Done;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import lombok.Value;

import java.util.Optional;

public interface PContactCommand {

    @Value
    final class CreateContact implements PContactCommand, PersistentEntity.ReplyType<Done> {
        PContact pContact;

        @JsonCreator
        public CreateContact (PContact pContact) {
            this.pContact = pContact;
        }

    }

    @Value
    final class UpdateContact implements PContactCommand, PersistentEntity.ReplyType<PContact> {
        PContactData pContactData;

        @JsonCreator
        public UpdateContact(PContactData pContactData){
            this.pContactData = pContactData;
        }
    }


    enum GetContact implements PContactCommand, PersistentEntity.ReplyType<Optional<PContact>> {
        INSTANCE;
    }
}
