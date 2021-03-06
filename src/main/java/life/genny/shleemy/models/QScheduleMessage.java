package life.genny.shleemy.models;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.JsonObject;
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
import life.genny.shleemy.utils.LocalDateTimeAdapter;

@Entity
@Cacheable
@Table(name = "schedulemessage")
@RegisterForReflection
public class QScheduleMessage extends PanacheEntity {

	 private static final Logger log = Logger.getLogger(QScheduleMessage.class);	
	 private static final String DEFAULT_TAG = "default";

	@JsonbTypeAdapter(LocalDateTimeAdapter.class)
	public LocalDateTime created = LocalDateTime.now(ZoneId.of("UTC"));
	@JsonbTypeAdapter(LocalDateTimeAdapter.class)
	public LocalDateTime updated;

	
	
	public String cron;
	

	public LocalDateTime triggertime;

		
	@NotEmpty
	public String realm;

	@NotEmpty
	@Column(name = "jsonMessage", columnDefinition = "LONGTEXT")
	public String jsonMessage;
	
	
	@NotEmpty
	public String sourceCode;

	@NotEmpty
	public String channel;
	
	@Column(name = "token", columnDefinition = "MEDIUMTEXT")
	public String token;
	
	public String code;


	public QScheduleMessage()
	{}
	
	public QScheduleMessage(final String code,final String jsonMessage, final String sourceCode, final String channel, final String cron, final String realm)
	{
		this.code = code;
		this.cron = cron;
		this.jsonMessage = jsonMessage;
		this.channel = channel;
		this.sourceCode = sourceCode;
	}
	
	public QScheduleMessage(final String code,final String jsonMessage, final String sourceCode, final String channel, final LocalDateTime triggertime, final String realm)
	{
		this.code = code;
		this.triggertime = triggertime;
		this.jsonMessage = jsonMessage;
		this.channel = channel;
		this.sourceCode = sourceCode;
	}
	
	public static QScheduleMessage findById(Long id) {
		return find("id", id).firstResult();
	}

	public static QScheduleMessage findByCode(String code) {
		return find("code", code).firstResult();
	}

	public static long deleteById(final Long id) {
		return delete("id", id);
	}

	public static long deleteByCode(final String code) {
		return delete("code", code);
	}
}
