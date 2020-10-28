package life.genny.shleemy.quartz;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import io.quarkus.runtime.StartupEvent;

import life.genny.shleemy.endpoints.ScheduleResource;
import life.genny.shleemy.models.GennyToken;
import life.genny.shleemy.models.QScheduleMessage;
import life.genny.shleemy.utils.WriteToBridge;

@ApplicationScoped
public class TaskBean {

	private static final Logger log = Logger.getLogger(ScheduleResource.class);

	@Inject
	org.quartz.Scheduler quartz;

	void onStart(@Observes StartupEvent event) throws SchedulerException {
//		JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity("myJob", "myGroup").build();
//		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("myTrigger", "myGroup").startNow()
//				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(10).repeatForever()).build();
//		quartz.scheduleJob(job, trigger);
	}

	public String addSchedule(QScheduleMessage scheduleMessage, GennyToken userToken) throws SchedulerException {

		scheduleMessage.token = userToken.getToken();

		String messageJson = scheduleMessage.jsonMessage;

		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("message", messageJson);
		jobDataMap.put("sourceCode", userToken.getUserCode());
		jobDataMap.put("token", userToken.getToken());
		jobDataMap.put("channel", scheduleMessage.channel);
		
		String uniqueCode = userToken.getUserCode()+"-"+UUID.randomUUID().toString().substring(0, 15);

		JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity(uniqueCode, userToken.getRealm())
				.setJobData(jobDataMap).build();

		Trigger trigger = null;

		if (!StringUtils.isBlank(scheduleMessage.cron)) {
			trigger = TriggerBuilder.newTrigger().withIdentity(uniqueCode, userToken.getRealm()).startNow()
					.withSchedule(cronSchedule(
							scheduleMessage.realm + ":" + scheduleMessage.sourceCode + ":" + userToken.getEmail(),
							scheduleMessage.cron))
					.build();
			log.info(
					"Scheduled " + userToken.getUserCode() + ":" + userToken.getEmail() + " for " + userToken.getRealm()
							+ " for trigger at " + scheduleMessage.cron + " and now is " + LocalDateTime.now());

		} else if (scheduleMessage.triggertime != null) {
			Date scheduledDateTime = Date.from(scheduleMessage.triggertime.atZone(ZoneId.systemDefault()).toInstant());
			trigger = TriggerBuilder.newTrigger().withIdentity("trigger1", userToken.getRealm())
					.startAt(scheduledDateTime) // some Date date 30.06.2017 12:30
					.forJob(uniqueCode, userToken.getRealm()) // identify job with name, group strings
					.build();
			log.info(
					"Scheduled " + userToken.getUserCode() + ":" + userToken.getEmail() + " for " + userToken.getRealm()
							+ " for trigger at " + scheduledDateTime + " and now is " + LocalDateTime.now());

		}

		quartz.scheduleJob(job, trigger);
		
		return uniqueCode;

	}

	private static CronScheduleBuilder cronSchedule(String desc, String cronExpression) {
		System.out.println(desc + "->(" + cronExpression + ")");
		return CronScheduleBuilder.cronSchedule(cronExpression);
	}

	@Transactional
	void performTask(JobExecutionContext context) {
		String bridgeUrl = ConfigProvider.getConfig().getValue("bridge.service.url", String.class);

		String sourceCode = context.getJobDetail().getJobDataMap().getString("sourceCode");
		String channel = context.getJobDetail().getJobDataMap().getString("channel");
		String token = context.getJobDetail().getJobDataMap().getString("token");
		GennyToken userToken = new GennyToken(token);

		log.info("Executing Schedule " + sourceCode + ":" + userToken.getEmail() + " for " + userToken.getRealm()
				+ " at " + LocalDateTime.now());

		String scheduleMsgJson = (String) context.getJobDetail().getJobDataMap().get("message");// jsonb.toJson(scheduleMessage);
		WriteToBridge.writeMessage(bridgeUrl, channel, scheduleMsgJson, userToken);

	}

	// A new instance of MyJob is created by Quartz for every job execution
	public static class MyJob implements Job {

		@Inject
		TaskBean taskBean;

		public void execute(JobExecutionContext context) throws JobExecutionException {
			taskBean.performTask(context);
		}

	}
}