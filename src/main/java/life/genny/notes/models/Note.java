package life.genny.notes.models;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.persistence.Cacheable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.notes.utils.LocalDateTimeAdapter;

@Entity
@Cacheable
@Table(name = "note")
@RegisterForReflection
public class Note extends PanacheEntity {

	 private static final Logger log = Logger.getLogger(Note.class);	
	 private static final String DEFAULT_TAG = "default";

	@JsonbTypeAdapter(LocalDateTimeAdapter.class)
	public LocalDateTime created = LocalDateTime.now(ZoneId.of("UTC"));
	@JsonbTypeAdapter(LocalDateTimeAdapter.class)
	public LocalDateTime updated;
// 
	@NotEmpty
	@JsonbTransient
	public String realm;

	// @FullTextField(analyzer = "english")
	@Column(name = "content")
	@NotEmpty
	public String content;
	
	// public Date updated = new Date();
	@ElementCollection(fetch=FetchType.EAGER)
	@Column(name = "note_tag")
    @CollectionTable(name = "tag")
	@JoinColumn(name = "note_id")
	@OnDelete(action= OnDeleteAction.CASCADE)
	//@JsonbTypeAdapter(TagsAdapter.class)
	public Set<Tag> tags = new HashSet<>();

	
	@NotEmpty
	public String sourceCode;

	@NotEmpty
	public String targetCode;


	public Note() {
    }

	public static Note findById(Long id) {
		return find("id", id).firstResult();
	}

	public static long deleteById(final Long id) {
		return delete("id", id);
	}

	public static QDataNoteMessage findByTags(final GennyToken userToken, final List<Tag> tags, Page page) {
		List<String> tagStringList = tags.stream().collect(Collectors.mapping(p -> p.getName(), Collectors.toList()));

		PanacheQuery<Note> notes = null;
		Long total = 0L;
		if (!tagStringList.isEmpty()) {
			notes = Note.find(
					"select n from Note n JOIN n.tags t where n.realm = :realm and t.name in (:tags)  order by n.created",
					Parameters.with("realm", userToken.getRealm()).and("tags", tagStringList));
			
			if (notes.count()>0 ) {
				total = Note.count("from Note n JOIN n.tags t where n.realm = :realm and t.name in (:tags)",
						Parameters.with("realm",  userToken.getRealm()).and("tags", tagStringList));
			}
		} else {
			notes = Note.find("select n from Note n  where n.realm = :realm  order by n.created",
					Parameters.with("realm",  userToken.getRealm()));
			if (notes.count()>0 ) {
				total = Note.count("realm", userToken.getRealm());
			}

		}		
		
		QDataNoteMessage noteMsg = new QDataNoteMessage( notes.page(page).list(),total);
		return noteMsg;
	}

	public static QDataNoteMessage findByTargetAndTags(final GennyToken userToken, final List<Tag> tags, final String targetCode,
			Page page) {
		List<String> tagStringList = tags.stream().collect(Collectors.mapping(p -> p.getName(), Collectors.toList()));

		PanacheQuery<Note> notes = null;
		Long total = 0L;


		if (!tagStringList.isEmpty()) {
			notes = Note.find(
					"select n from Note n JOIN n.tags t where n.realm = :realm and t.name in (:tags) and n.targetCode = :targetCode  order by n.created",
					Parameters.with("realm",  userToken.getRealm()).and("targetCode", targetCode).and("tags", tagStringList));
			if (notes.count()>0 ) {
				total = Note.count("from Note n JOIN n.tags t where n.realm = :realm and t.name in (:tags) and n.targetCode = :targetCode ",
						Parameters.with("realm",  userToken.getRealm()).and("targetCode", targetCode).and("tags", tagStringList));
			}

		} else {
			notes = Note.find(
					"select n from Note n  where n.realm = :realm  and n.targetCode = :targetCode  order by n.created",
					Parameters.with("realm",  userToken.getRealm()).and("targetCode", targetCode));
			if (notes.count()>0 ) {
				total = Note.count("realm = :realm  and targetCode = :targetCode",
						Parameters.with("realm",  userToken.getRealm()).and("targetCode", targetCode));
			}

		}
		
		QDataNoteMessage noteMsg = new QDataNoteMessage( notes.page(page).list(),total);
		return noteMsg;


	}
}
