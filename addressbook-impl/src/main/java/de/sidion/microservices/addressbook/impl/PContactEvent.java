package de.sidion.microservices.addressbook.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventShards;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTagger;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.Value;

import java.util.UUID;

public interface PContactEvent extends AggregateEvent<PContactEvent>, Jsonable {

    int NUM_SHARDS = 4;
    AggregateEventShards<PContactEvent> TAG = AggregateEventTag.sharded(PContactEvent.class, NUM_SHARDS);

    @Override
    default AggregateEventTagger<PContactEvent> aggregateTag() {
        return TAG;
    }

    @Value
    final class ContactCreated implements PContactEvent {
        PContact pContact;

        @JsonCreator
        ContactCreated(PContact pContact) {
            this.pContact = pContact;
        }
    }

    @Value
    final class ContactUpdated implements PContactEvent {
        UUID contactId;
        PContactData contactDetails;

        @JsonCreator
        ContactUpdated(UUID contactId, PContactData contactDetails){
            this.contactId = contactId;
            this.contactDetails = contactDetails;
        }
    }
}
