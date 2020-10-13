package life.genny.notes.endpoints;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.security.identity.SecurityIdentity;
import life.genny.notes.models.DataTable;
import life.genny.notes.models.GennyToken;
import life.genny.notes.models.Note;
import life.genny.notes.models.NoteStatus;
import life.genny.notes.models.ParentNote;
import life.genny.notes.models.QDataNoteMessage;
import life.genny.notes.models.Tag;
import life.genny.notes.utils.WriteToBridge;

@Path("/v7/notes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NoteResource {

	private static final Logger log = Logger.getLogger(Note.class);

	@ConfigProperty(name = "default.realm", defaultValue = "genny")
	String defaultRealm;

	
	@Inject
	SecurityIdentity securityIdentity;

	@Inject
	JsonWebToken accessToken;

	@OPTIONS
	public Response opt() {
		return Response.ok().build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNotesByTags(@QueryParam("tags") String tags,
			@QueryParam("pageIndex") @DefaultValue("0") Integer pageIndex,
			@QueryParam("pageSize") @DefaultValue("20") Integer pageSize) {

		GennyToken userToken = new GennyToken(accessToken.getRawToken());
		log.info("GennyToken = " + userToken);

		List<Tag> tagList = null;
		if (tags != null) {
			List<String> tagStringList = Arrays.asList(StringUtils.splitPreserveAllTokens(tags, ","));
			tagList = tagStringList.stream().collect(Collectors.mapping(p -> new Tag(p), Collectors.toList()));
		} else {
			tagList = new ArrayList<Tag>();
		}
		QDataNoteMessage notes = Note.findByTags(userToken, tagList, Page.of(pageIndex, pageSize));

		return Response.status(Status.OK).entity(notes).build();
	}

	@Transactional
	@POST
	public Response newNote(@Context UriInfo uriInfo, @Valid Note note) {
		note.id = null;
		GennyToken userToken = new GennyToken(accessToken.getRawToken());
		log.info("GennyToken = " + userToken);
		note.realm = userToken.getRealm();
		note.sourceCode = userToken.getUserCode(); // force

		note.persist();
		
		processParentNote(userToken, note.targetCode);


		notifyNoteGroup(note, NoteStatus.NEW);

		URI uri = uriInfo.getAbsolutePathBuilder().path(NoteResource.class, "findById").build(note.id);
		return Response.created(uri).build();
	}

	private void notifyNoteGroup(Note note, NoteStatus noteStatus) {

		GennyToken userToken = new GennyToken(accessToken.getRawToken());
		ParentNote parentNote = ParentNote.findByCode(note.targetCode);

		if (parentNote != null) {
			String bridgeUrl = "http://erstwhile-wolf-genny-bridge-svc/api/service";//ConfigProvider.getConfig().getValue("quarkus.bridge.service.url", String.class);


			QDataNoteMessage msg = new QDataNoteMessage(note, noteStatus);
			msg.setRecipientCodeArray(parentNote.noteUsers.toArray(new String[0]));
			log.info("Writing "+msg+" using "+bridgeUrl);


			WriteToBridge.writeMessage(bridgeUrl, msg, userToken);
		} else {
			log.error("ParentNote is null for notes "+note.targetCode);
		}

	}

	@Path("/id/{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response findById(@PathParam("id") final Long id) {
		GennyToken userToken = new GennyToken(accessToken.getRawToken());
		log.info("GennyToken = " + userToken);


		Note note = Note.findById(id);
		if (note == null) {
			throw new WebApplicationException("Note with id of " + id + " does not exist.", Status.NOT_FOUND);
		}
		if (note.realm != userToken.getRealm()) {
			throw new WebApplicationException(
					"Note with id of " + id + " does not exist in your realm ." + userToken.getRealm(),
					Status.NOT_FOUND);
		}
		processParentNote(userToken, note.targetCode);

		return Response.status(Status.OK).entity(note).build();
	}

	@Path("/{targetCode}")
	@GET
	@Transactional
	public Response getNotesByTargetCodeAndTags(@PathParam("targetCode") final String targetCode,
			@QueryParam("tags") @DefaultValue("") String tags,
			@QueryParam("pageIndex") @DefaultValue("0") Integer pageIndex,
			@QueryParam("pageSize") @DefaultValue("20") Integer pageSize) {
		// Object userName = this.idToken.getClaim("preferred_username");

		GennyToken userToken = new GennyToken(accessToken.getRawToken());
		log.info("GennyToken = " + userToken);
		processParentNote(userToken, targetCode);

		// TODO - Check user access security here.

		List<String> tagStringList = Arrays.asList(StringUtils.splitPreserveAllTokens(tags, ","));
		List<Tag> tagList = tagStringList.stream().collect(Collectors.mapping(p -> new Tag(p), Collectors.toList()));
		QDataNoteMessage notes = Note.findByTargetAndTags(userToken, tagList, targetCode, Page.of(pageIndex, pageSize));

		return Response.status(Status.OK).entity(notes).build();
	}

	/**
	 * @param userToken
	 * @param targetCode
	 */
	private void processParentNote(GennyToken userToken, final String targetCode) {
		if (!userToken.getString("preferred_username").equals("service")) { // don't worry if system / service
		ParentNote parentNote = ParentNote.findByCode(targetCode);

		if (parentNote == null) {
				parentNote = new ParentNote(userToken.getRealm(), targetCode, userToken.getUserCode());
				parentNote.persist();
				log.info("New ParentNote for "+targetCode+" created and adding "+userToken.getUserCode());
		} else {
			if (!parentNote.noteUsers.contains(userToken.getUserCode())) {
				parentNote.noteUsers.add(userToken.getUserCode());
				parentNote.persist();
				log.info("Updating ParentNote for "+targetCode+" added "+userToken.getUserCode());
			}
		}
		}
	}

	@Path("/{id}")
	@PUT
	@Transactional
	public Response updateNote(@PathParam("id") final Long id, @Valid Note note) {
		GennyToken userToken = new GennyToken(accessToken.getRawToken());
		log.info("GennyToken = " + userToken);

		Note existed = Note.findById(id);
		if (existed == null) {
			throw new WebApplicationException("Note with id of " + id + " does not exist.", Status.NOT_FOUND);
		}
//		if (existed.realm != userToken.getRealm()) {
//			throw new WebApplicationException(
//					"Note with id of " + id + " does not exist in your realm ." + userToken.getRealm(),
//					Status.NOT_FOUND);
//		}

		existed.content = note.content;
		existed.updated = LocalDateTime.now();
		existed.persist();
		
		processParentNote(userToken, existed.targetCode);


		notifyNoteGroup(existed, NoteStatus.UPDATED);

		return Response.status(Status.OK).entity(existed).build();
	}

	@Path("/{id}")
	@DELETE
	@Transactional
	public Response deleteNote(@PathParam("id") final Long id) {
		GennyToken userToken = new GennyToken(accessToken.getRawToken());
		log.info("GennyToken = " + userToken);

		Note existed = Note.findById(id);
		if (existed == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		log.info("Existing realm = [" + existed.realm + "] , userToken realm = [" + userToken.getRealm() + "]");
//		if (existed.realm != userToken.getRealm()) {
//			throw new WebApplicationException(
//					"Note with id of " + id + " does not exist in your realm " + userToken.getRealm(),
//					Status.NOT_FOUND);
//		}
		if ((userToken.hasRole("admin")) || (userToken.getUserCode().equals(existed.sourceCode))) {
			Note.deleteById(id);
		} else {
			throw new WebApplicationException("You do not have permission to delete this note", Status.FORBIDDEN);

		}
		processParentNote(userToken, existed.targetCode);

		notifyNoteGroup(existed, NoteStatus.DELETED);

		return Response.status(Status.OK).build();
	}

	@GET
	@Path("/datatable")
	@Produces(MediaType.APPLICATION_JSON)
	public DataTable<Note> datatable(@QueryParam(value = "draw") int draw, @QueryParam(value = "start") int start,
			@QueryParam(value = "length") int length, @QueryParam(value = "search[value]") String searchVal

	) {
		GennyToken userToken = new GennyToken(accessToken.getRawToken());
		log.info("GennyToken = " + userToken);

		life.genny.notes.models.DataTable<Note> result = new DataTable<>();
		if (userToken.hasRole("admin")) {

			searchVal = "";
			result.setDraw(draw);

			PanacheQuery<Note> filteredDevice;

			if (searchVal != null && !searchVal.isEmpty()) {
				filteredDevice = Note.<Note>find("content like :search",
						Parameters.with("search", "%" + searchVal + "%"));
			} else {
				filteredDevice = Note.findAll();
			}

			int page_number = 0;
			if (length > 0) {
				page_number = start / length;
			}
			filteredDevice.page(page_number, length);

			log.info("/datatable: search=[" + searchVal + "],start=" + start + ",length=" + length + ",result#="
					+ filteredDevice.count());

			result.setRecordsFiltered(filteredDevice.count());
			result.setData(filteredDevice.list());
			result.setRecordsTotal(Note.count());
		}
		return result;

	}

	@Transactional
	void onStart(@Observes StartupEvent ev) {
		log.info("Note Endpoint starting");
		// log.info("MySQL Password = " + mysqlPassword);
		// Creating some test
		// Fetch the base entities
//		BaseEntity sourceBE = (BaseEntity) em
//				.createQuery("SELECT be FROM BaseEntity be where be.realm=:realmStr and be.code=:code")
//				.setParameter("realmStr", "internmatch").setParameter("code", "PER_USER1").getSingleResult();
//
//		if (sourceBE != null) {
//
//			if (Note.count() == 0) {
//
//				Set<Tag> tags1 = Stream.of(new Tag("phone", 1), new Tag("intern", 3)).collect(Collectors.toSet());
//
//				Set<Tag> tags2 = Stream.of(new Tag("intern", 1), new Tag("rating", 5)).collect(Collectors.toSet());
//
//				Note test1 = new Note(defaultRealm, sourceBE, sourceBE, tags1, "This is the first note!");
//				test1.persist();
//
//				Note test2 = new Note(defaultRealm, sourceBE, sourceBE, tags2, "This is the second note!");
//				test2.persist();
//			}
//		} else {
//			log.error("No Baseentitys set up yet in Database");
//		}

	}

	@Transactional
	void onShutdown(@Observes ShutdownEvent ev) {
		log.info("Note Endpoint Shutting down");
	}
}