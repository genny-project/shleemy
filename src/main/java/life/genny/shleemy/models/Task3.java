package life.genny.shleemy.models;

import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;

import org.jboss.logging.Logger;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import life.genny.shleemy.utils.LocalDateTimeAdapter;

@Entity
@Table(name = "QTASKS3")
public class Task3 extends PanacheEntity {
	private static final Logger log = Logger.getLogger(Task3.class);
	private static final String DEFAULT_TAG = "default";

	@JsonbTypeAdapter(LocalDateTimeAdapter.class)
	public LocalDateTime created = LocalDateTime.now(ZoneId.of("UTC"));
	@JsonbTypeAdapter(LocalDateTimeAdapter.class)
	public LocalDateTime updated;
	//
	@NotEmpty
	@JsonbTransient
	public String realm;
}