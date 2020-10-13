package life.genny.notes.models;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

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
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.notes.utils.LocalDateTimeAdapter;

@Entity
@Cacheable
@Table(name = "parentnote")
@RegisterForReflection
public class ParentNote extends PanacheEntity {

	 private static final Logger log = Logger.getLogger(ParentNote.class);	

	@JsonbTypeAdapter(LocalDateTimeAdapter.class)
	public LocalDateTime created = LocalDateTime.now(ZoneId.of("UTC"));
	@JsonbTypeAdapter(LocalDateTimeAdapter.class)
	public LocalDateTime updated;
// 
	@NotEmpty
	@JsonbTransient
	public String realm;

	
	@NotEmpty
	public String code;


	@ElementCollection(fetch=FetchType.EAGER)
	@Column(name = "note_users")
    @CollectionTable(name = "note_reg_users")
	@JoinColumn(name = "noteuser_id")
	@OnDelete(action= OnDeleteAction.CASCADE)
	@JsonbTransient
	public Set<String> noteUsers = new HashSet<>();



	public ParentNote() {
    }

	public ParentNote(final String realm, final String code, final String noteUser)
	{
		this.realm = realm;
		this.code = code;
		noteUsers.add(noteUser);
	}
	
	public static ParentNote findById(Long id) {
		return find("id", id).firstResult();
	}
	
	public static ParentNote findByCode(String code) {
		ParentNote pn = null;
		
		try {
			pn = find("code", code).firstResult();
		} catch (Exception e) {
			
		}
		
		return pn;
	}


	public static long deleteById(final Long id) {
		return delete("id", id);
	}

	public static long deleteByCode(final String code) {
		return delete("code", code);
	}

}
