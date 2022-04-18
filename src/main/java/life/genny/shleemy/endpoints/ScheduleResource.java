package life.genny.shleemy.endpoints;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.security.identity.SecurityIdentity;
import java.net.URI;
import java.util.List;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import life.genny.shleemy.models.GennyToken;
import life.genny.shleemy.models.QScheduleMessage;
import life.genny.shleemy.quartz.TaskBean;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.quartz.SchedulerException;





@Path("/api/schedule")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScheduleResource {

	private static final Logger log = Logger.getLogger(ScheduleResource.class);

	@ConfigProperty(name = "default.realm", defaultValue = "genny")
	String defaultRealm;

		@ConfigProperty(name = "quarkus.oidc.realm", defaultValue = "internmatch")
	String realm;

	@ConfigProperty(name = "quarkus.oidc.auth-server-url", defaultValue = "https://keycloak.gada.io/auth")
	String keycloakUrl;

		@ConfigProperty(name = "quarkus.oidc.client-id", defaultValue = "mentormatch")
	String clientId;

		@ConfigProperty(name = "quarkus.oidc.credentials.secret", defaultValue = "")
	String secret;


	@Inject
	SecurityIdentity securityIdentity;

	@Context 
	HttpHeaders headers;

	// @Inject
	// JsonWebToken accessToken;

	@Inject
	TaskBean taskBean;

	@OPTIONS
	public Response opt() {
		return Response.ok().build();
	}

	@Transactional
	@POST
	public Response newQScheduleMessage(@Context UriInfo uriInfo, @Valid QScheduleMessage scheduleMessage) {
		String uniqueScheduleCode = "";

		scheduleMessage.id = null;
		List<String> authHeaders = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
		if ((authHeaders != null) && (!authHeaders.isEmpty())) {
			String token = authHeaders.get(0);
			log.info("token="+token);
			GennyToken userToken = new GennyToken(token);
			log.info("User is " + userToken.getEmail());

			if (!(userToken.hasRole("admin") || userToken.hasRole("service") || userToken.hasRole("dev"))) {
				//log.error(userToken.getUserCode() + " has no authority to schedule");
				// return Response.status(Status.FORBIDDEN).entity("No authority to
				// schedule").build();
			}

			scheduleMessage.realm = userToken.getRealm();
			scheduleMessage.sourceCode = userToken.getUserCode(); // force
			scheduleMessage.token = userToken.getToken();
			scheduleMessage.persist();

			log.info("Persisting new Schedule-> " + scheduleMessage.code + ":" + scheduleMessage.triggertime + " from "
					+ scheduleMessage.sourceCode);

			try {
				uniqueScheduleCode = taskBean.addSchedule(scheduleMessage, userToken);
				URI uri = uriInfo.getAbsolutePathBuilder().path(ScheduleResource.class, "findById")
						.build(scheduleMessage.id);
				return Response.created(uri).entity(uniqueScheduleCode).build();

			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return Response.status(Status.BAD_REQUEST).entity("ScheduleMessage did not schedule").build();
		} else {
			return Response.status(Status.FORBIDDEN).build();
		}
	}

	@Path("/code/{code}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response findByCode(@PathParam("code") final String code) {
		List<String> authHeaders = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
		if ((authHeaders != null) && (!authHeaders.isEmpty())) {
			String token = authHeaders.get(0);

			GennyToken userToken = new GennyToken(token);
			log.info("User is " + userToken.getEmail());
			if (!(userToken.hasRole("admin") || userToken.hasRole("service") || userToken.hasRole("dev"))) {
				log.error(userToken.getUserCode() + " has no authority to schedule");
				// return Response.status(Status.FORBIDDEN).entity("No authority to
				// schedule").build();
			}

			QScheduleMessage scheduleMessage = QScheduleMessage.findByCode(code);
			if (scheduleMessage == null) {
				return Response.status(Status.NOT_FOUND)
						.entity("ScheduleMessage with code of " + code + " does not exist.")
						.build();
			}
			if (scheduleMessage.realm != userToken.getRealm()) {
				return Response.status(Status.NOT_FOUND).entity("ScheduleMessage with bad realm")
						.build();
			}

			return Response.status(Status.OK).entity(scheduleMessage).build();
		} else {
return Response.status(Status.FORBIDDEN).build();
		}
	}
	
	@Path("/id/{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response findById(@PathParam("id") final Long id) {
				List<String> authHeaders = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
		if ((authHeaders != null) && (!authHeaders.isEmpty())) {
			String token = authHeaders.get(0);

		GennyToken userToken = new GennyToken(token);
		log.info("User is " + userToken.getEmail());
		if (!(userToken.hasRole("admin") || userToken.hasRole("service") || userToken.hasRole("dev"))) {
			log.error(userToken.getUserCode() + " has no authority to schedule");
			// return Response.status(Status.FORBIDDEN).entity("No authority to
			// schedule").build();
		}

		QScheduleMessage scheduleMessage = QScheduleMessage.findById(id);
		if (scheduleMessage == null) {
			return Response.status(Status.NOT_FOUND).entity("ScheduleMessage with id of " + id + " does not exist.")
					.build();
		}
		if (scheduleMessage.realm != userToken.getRealm()) {
			return Response.status(Status.NOT_FOUND).entity("ScheduleMessage with id of " + id + " does not exist.")
					.build();
		}

		return Response.status(Status.OK).entity(scheduleMessage).build();
				} else {
return Response.status(Status.FORBIDDEN).build();
		}
	}

	@Path("/{id}")
	@DELETE
	@Transactional
	public Response deleteSchedule(@PathParam("id") final Long id) {
						List<String> authHeaders = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
		if ((authHeaders != null) && (!authHeaders.isEmpty())) {
			String token = authHeaders.get(0);

		GennyToken userToken = new GennyToken(token);
		log.info("User is " + userToken.getEmail());
		if (!(userToken.hasRole("admin") || userToken.hasRole("service") || userToken.hasRole("dev"))) {
			log.error(userToken.getUserCode() + " has no authority to schedule");
			// return Response.status(Status.FORBIDDEN).entity("No authority to
			// schedule").build();
		}

		QScheduleMessage scheduleMessage = QScheduleMessage.findById(id);
		if (scheduleMessage == null) {
			return Response.status(Status.NOT_FOUND).entity("ScheduleMessage with id of " + id + " does not exist.")
					.build();
		}
		if (scheduleMessage.realm != userToken.getRealm()) {
			return Response.status(Status.NOT_FOUND).entity("ScheduleMessage with id of " + id + " does not exist.")
					.build();
		}
		if ((userToken.hasRole("admin")) || (userToken.getUserCode().equals(scheduleMessage.sourceCode))) {
			// QScheduleMessage.deleteById(id);
		} else {
			return Response.status(Status.FORBIDDEN)
					.entity("ScheduleMessage with id of " + id + " cannot be deleted by this user.").build();

		}

		QScheduleMessage msg = QScheduleMessage.findById(id);
		if (msg != null) {
				Boolean success = false;
				try {
					success = taskBean.abortSchedule(msg.code, userToken);
				} catch (org.quartz.SchedulerException e) {
					log.error(e.getMessage());
				}

				if (success) {
					QScheduleMessage.deleteByCode(msg.code);
					return Response.status(Status.OK).build();
				} else {
					return Response.status(Status.NOT_FOUND).build();
				}
		}

		return Response.status(Status.OK).build();
				} else {
return Response.status(Status.FORBIDDEN).build();
		}
	}

	@Path("/code/{code}")
	@DELETE
	@Transactional
	public Response deleteSchedule(@PathParam("code") final String code) {
						List<String> authHeaders = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
		if ((authHeaders != null) && (!authHeaders.isEmpty())) {
			String token = authHeaders.get(0);

		GennyToken userToken = new GennyToken(token);
		log.info("User is " + userToken.getEmail());
		if (!(userToken.hasRole("admin") || userToken.hasRole("service") || userToken.hasRole("dev"))) {
			log.error(userToken.getUserCode() + " has no authority to schedule");
			// return Response.status(Status.FORBIDDEN).entity("No authority to
			// schedule").build();
		}

		QScheduleMessage scheduleMessage = QScheduleMessage.findByCode(code);
		if (scheduleMessage == null) {
			return Response.status(Status.NOT_FOUND).entity("ScheduleMessage with code of " + code + " does not exist.")
					.build();
		}
		if (scheduleMessage.realm != userToken.getRealm()) {
			return Response.status(Status.NOT_FOUND).entity("ScheduleMessage has a different realm to user.").build();
		}
		if ((userToken.hasRole("admin")) || (userToken.getUserCode().equals(scheduleMessage.sourceCode))) {
			// QScheduleMessage.deleteByCode(code);
		} else {
			return Response.status(Status.FORBIDDEN)
					.entity("ScheduleMessage with code of " + code + " cannot be deleted by this user.").build();

		}

		
		Boolean success = false;
		try {
			success = taskBean.abortSchedule(code, userToken);
		} catch (org.quartz.SchedulerException e) {
			log.error(e.getMessage());
		}
		if (success) {
			QScheduleMessage.deleteByCode(code);
			return Response.status(Status.OK).build();
		} else {
			return Response.status(Status.NOT_FOUND).build();
		}
						} else {
return Response.status(Status.FORBIDDEN).build();
		}
	}

	@Transactional
	void onStart(@Observes StartupEvent ev) {
		log.info("ScheduleResource Endpoint starting");

		log.info("realm="+realm);
		log.info("keycloakUrl=" + keycloakUrl);
		log.info("clientId=" + clientId);
		log.info("secret="+secret);

	}

	@Transactional
	void onShutdown(@Observes ShutdownEvent ev) {
		log.info("ScheduleResource Endpoint Shutting down");
	}
}