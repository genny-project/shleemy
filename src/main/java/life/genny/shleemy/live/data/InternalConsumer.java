package life.genny.shleemy.live.data;

import org.jboss.logging.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;
import life.genny.qwandaq.data.GennyCache;
import life.genny.qwandaq.message.QScheduleMessage;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.shleemy.utils.ScheduleUtils;
import io.quarkus.runtime.ShutdownEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

@ApplicationScoped
public class InternalConsumer {

	private static final Logger log = Logger.getLogger(InternalConsumer.class);

	@ConfigProperty(name = "genny.keycloak.url", defaultValue = "https://keycloak.gada.io")
	String keycloakUrl;

	@ConfigProperty(name = "genny.keycloak.realm", defaultValue = "genny")
	String keycloakRealm;

	@ConfigProperty(name = "genny.service.username", defaultValue = "service")
	String serviceUsername;

	@ConfigProperty(name = "genny.service.password", defaultValue = "password")
	String servicePassword;

	@ConfigProperty(name = "genny.oidc.client-id", defaultValue = "backend")
	String clientId;

	@ConfigProperty(name = "genny.oidc.credentials.secret", defaultValue = "secret")
	String secret;

	@Inject
	GennyCache cache;

	GennyToken serviceToken;

	BaseEntityUtils beUtils;

	Jsonb jsonb = JsonbBuilder.create();

    void onStart(@Observes StartupEvent ev) {

        log.info("The Consumer is starting...");

		// Fetch our service token
		serviceToken = new KeycloakUtils().getToken(keycloakUrl, keycloakRealm, clientId, secret, serviceUsername, servicePassword, null);

		// Init Utility Objects
		beUtils = new BaseEntityUtils(serviceToken);

		// Establish connection to cache and init utilities
		CacheUtils.init(cache);
		QwandaUtils.init(serviceToken);
		DefUtils.init(beUtils);

		log.info("[*] Finished Consumer Startup!");
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("The application is stopping...");
    }

	@Incoming("schedule")
	public void getFromSchedule(String payload) {

		log.debug("Incoming Messsage: " + payload);

		QScheduleMessage msg = jsonb.fromJson(payload, QScheduleMessage.class);

		ScheduleUtils.scheduleMessage(msg);
	}
}
