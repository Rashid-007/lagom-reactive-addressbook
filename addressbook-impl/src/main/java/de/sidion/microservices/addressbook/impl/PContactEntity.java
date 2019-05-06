package de.sidion.microservices.addressbook.impl;

import akka.Done;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class PContactEntity extends PersistentEntity<PContactCommand, PContactEvent, PContactState> {
    @Override
    public Behavior initialBehavior(Optional<PContactState> snapshotState) {
        BehaviorBuilder builder = newBehaviorBuilder(snapshotState.orElse(PContactState.empty()));

        builder.setReadOnlyCommandHandler(PContactCommand.GetContact.class, this::getContact);

        // maybe do some validation? Eg, check that UUID of contact matches entity UUID...
        builder.setCommandHandler(PContactCommand.CreateContact.class, (create, ctx) ->
                ctx.thenPersist(new PContactEvent.ContactCreated(create.getPContact()), evt -> ctx.reply(Done.getInstance()))
        );
        builder.setEventHandlerChangingBehavior(PContactEvent.ContactCreated.class, evt -> created(PContactState.create(evt.getPContact())));
        builder.setReadOnlyCommandHandler(PContactCommand.UpdateContact.class, (updateContact, ctx) ->
                // TODO: avoid using a transport Exception on PersistentEntity
                ctx.commandFailed(new NotFound(entityId()))
        );

        return builder.build();
    }

    private Behavior created(PContactState state) {
        BehaviorBuilder builder = newBehaviorBuilder(state);

        builder.setReadOnlyCommandHandler(PContactCommand.GetContact.class, this::getContact);

        // must only emit an ContactUpdated when there's changes to be notified and the commander
        // is allowed to commit those changes.
        builder.setCommandHandler(PContactCommand.UpdateContact.class, (cmd, ctx) -> {
            PContact pContact = state().getPContact().get();
            return updateContact(cmd, ctx, pContact, () -> emitUpdatedEvent(cmd, ctx, pContact));
        });
        builder.setEventHandler(PContactEvent.ContactUpdated.class, updateContactData());

        return builder.build();
    }

    private Persist updateContact(PContactCommand.UpdateContact cmd, CommandContext<PContact> ctx, PContact pContact, Supplier<Persist> onSuccess) {
        if (!pContact.getPContactData().equals(cmd.getPContactData())) {
            return onSuccess.get();
        } else {
            // when update and current are equal there's no need to emit an event.
            return ctx.done();
        }
    }

    private Persist emitUpdatedEvent(PContactCommand.UpdateContact cmd, CommandContext<PContact> ctx, PContact ppContact) {
        return ctx.thenPersist(
                new PContactEvent.ContactUpdated(ppContact.getId(), cmd.getPContactData()),
                // when the command is accepted for processing we return a copy of the
                // state with the updates applied.
                evt -> ctx.reply(ppContact.withDetails(cmd.getPContactData())));
    }

    /**
     * convenience method to update the PContact in the PContactState with altering Instants, Status, etc...
     *
     * @return
     */
    private Function<PContactEvent.ContactUpdated, PContactState> updateContactData() {
        return (evt) -> state().updateDetails(evt.getContactDetails());
    }

    private void  getContact(PContactCommand.GetContact get, ReadOnlyCommandContext<Optional<PContact>> ctx) {
        ctx.reply(state().getPContact());
    }
}
