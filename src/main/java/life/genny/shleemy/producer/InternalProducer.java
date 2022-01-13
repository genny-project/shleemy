package life.genny.shleemy.producer;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * InternalProducer --- Kafka smalltye producer objects to send to internal consumers backends
 * such as wildfly-rulesservice.
 *
 * @author    hello@gada.io
 *
 */
@ApplicationScoped
public class InternalProducer {


    @Inject @Channel("eventsout") Emitter<String> events;
    public Emitter<String> getToEvents() {
        return events;
    }

}
