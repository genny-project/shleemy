package life.genny.shleemy.live.data;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class InternalProducer {

    @Inject @Channel("eventsout") Emitter<String> events;
    public Emitter<String> getToEvents() {
        return events;
    }

}
