package de.sidion.microservices.addressbook.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.Value;

import java.util.Optional;
import java.util.function.Function;

/**
 * looks like each state is corresponded to a contact.
 */
@Value
public class PContactState implements Jsonable {
    private final Optional<PContact> pContact;

    @JsonCreator
    public PContactState(Optional<PContact> pContact) {
        this.pContact = pContact;
    }

    public static PContactState empty() {
        return new PContactState(Optional.empty());
    }

    public static PContactState create(PContact pContact) {
        return new PContactState(Optional.of(pContact));
    }

    public PContactState updateDetails(PContactData details) {
        return update(i -> i.withDetails(details));
    }

    private PContactState update(Function<PContact, PContact> updateFunction) {
        assert pContact.isPresent();
        return new PContactState(pContact.map(updateFunction));
    }

}
