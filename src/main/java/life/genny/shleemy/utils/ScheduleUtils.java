package life.genny.shleemy.utils;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.quartz.SchedulerException;

import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.message.QScheduleMessage;
import life.genny.shleemy.quartz.TaskBean;

public class ScheduleUtils {

	private static final Logger log = Logger.getLogger(ScheduleUtils.class);

	@Inject
	static JsonWebToken accessToken;

	@Inject
	static TaskBean taskBean;

	public static void scheduleMessage(QScheduleMessage scheduleMessage) {

		scheduleMessage.id = null;
		GennyToken userToken = new GennyToken(accessToken.getRawToken());
		log.info("User is " + userToken.getEmail());

		scheduleMessage.realm = userToken.getRealm();
		scheduleMessage.sourceCode = userToken.getUserCode();
		scheduleMessage.token = userToken.getToken();
		scheduleMessage.persist();

		log.info("Persisting new Schedule-> "+scheduleMessage.code+":"+scheduleMessage.triggertime+" from "+scheduleMessage.sourceCode);
		
		try {
			taskBean.addSchedule(scheduleMessage, userToken);
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}

}
