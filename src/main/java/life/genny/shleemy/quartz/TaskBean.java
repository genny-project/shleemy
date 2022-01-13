package life.genny.shleemy.quartz;

import io.quarkus.runtime.StartupEvent;
import life.genny.shleemy.endpoints.ScheduleResource;
import life.genny.shleemy.models.GennyToken;
import life.genny.shleemy.models.QScheduleMessage;
import life.genny.shleemy.producer.InternalProducer;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.quartz.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@ApplicationScoped
public class TaskBean {

	private static final Logger log = Logger.getLogger(ScheduleResource.class);

	@Inject
	org.quartz.Scheduler quartz;

	@Inject
	InternalProducer producer;

	void onStart(@Observes StartupEvent event) throws SchedulerException {
//		JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity("myJob", "myGroup").build();
//		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("myTrigger", "myGroup").startNow()
//				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(10).repeatForever()).build();
//		quartz.scheduleJob(job, trigger);
	}

	
	public Boolean abortSchedule(String uniqueCode, GennyToken userToken) throws SchedulerException {
		Boolean ret = true;
	
		JobKey jobKey = new JobKey(uniqueCode, userToken.getRealm());
		quartz.deleteJob(jobKey);
		return ret;
	}
	
	public String addSchedule(QScheduleMessage scheduleMessage, GennyToken userToken) throws SchedulerException {

		scheduleMessage.token = userToken.getToken();

		String messageJson = scheduleMessage.jsonMessage;

		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("message", messageJson);
		jobDataMap.put("sourceCode", userToken.getUserCode());
		jobDataMap.put("token", userToken.getToken());
		jobDataMap.put("channel", scheduleMessage.channel);
		jobDataMap.put("code", scheduleMessage.code);
		
		String uniqueCode = scheduleMessage.code; // given to us by sender

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
			trigger = TriggerBuilder.newTrigger().withIdentity(uniqueCode, userToken.getRealm())
					.startAt(scheduledDateTime) // some Date date 30.06.2017 12:30
					.forJob(uniqueCode, userToken.getRealm()) // identify job with name, group strings
					.build();
			log.info(
					"Scheduled " + userToken.getUserCode() +":"+uniqueCode+ ":" + userToken.getEmail() + " for " + userToken.getRealm()
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
		String code = context.getJobDetail().getJobDataMap().getString("code");
		String token = context.getJobDetail().getJobDataMap().getString("token");
		GennyToken userToken = new GennyToken(token);

		String scheduleMsgJson = (String) context.getJobDetail().getJobDataMap().get("message");// jsonb.toJson(scheduleMessage);
		producer.getToEvents().send(scheduleMsgJson);
//		String result = WriteToBridge.writeMessage(bridgeUrl, channel, scheduleMsgJson, userToken);
		log.info("Executing Schedule " + sourceCode + ":"+code+":" + userToken.getEmail() + " for " + userToken.getRealm()
				+ " at " + LocalDateTime.now()+" sending through bridgeUrl="+bridgeUrl + ", scheduleMsgJson:" + scheduleMsgJson);
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