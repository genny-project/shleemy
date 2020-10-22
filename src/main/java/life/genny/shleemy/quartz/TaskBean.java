package life.genny.shleemy.quartz;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.transaction.Transactional;

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

	public void addSchedule(QScheduleMessage scheduleMessage, final String cron, GennyToken userToken)  throws SchedulerException  {
		
		scheduleMessage.token = userToken.getToken();
		
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("message", scheduleMessage);
		jobDataMap.put("sourceCode", userToken.getUserCode());
		jobDataMap.put("token", userToken.getToken());
		
		
		JobDetail job = JobBuilder.newJob(MyJob.class)
				.withIdentity("myJob", userToken.getRealm())
				.setJobData(jobDataMap)
				.build();
		
		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("myTrigger", userToken.getRealm()).startNow()
				.withSchedule(cronSchedule(scheduleMessage.realm+":"+scheduleMessage.sourceCode+":"+userToken.getEmail(),
                        cron)).build();
		
		quartz.scheduleJob(job, trigger);
	}

	  private static CronScheduleBuilder cronSchedule(String desc, String cronExpression) {
	        System.out.println(desc + "->(" + cronExpression + ")");
	        return CronScheduleBuilder.cronSchedule(cronExpression);
	    }
	  
	@Transactional
	void performTask(JobExecutionContext context) {
//		Task task = new Task();
//		task.persist();
		
		String bridgeUrl = ConfigProvider.getConfig().getValue("bridge.service.url", String.class);

		String sourceCode = context.getJobDetail().getJobDataMap().getString("souceCode");
		String token = context.getJobDetail().getJobDataMap().getString("token");
		QScheduleMessage scheduleMessage = (QScheduleMessage)context.getJobDetail().getJobDataMap().get("message");
		Jsonb jsonb = JsonbBuilder.create();		 
		String scheduleMsgJson = jsonb.toJson(scheduleMessage);
		GennyToken userToken =  new GennyToken(token);
		WriteToBridge.writeMessage(bridgeUrl, scheduleMsgJson, userToken);
		log.info("Executing Schedule "+sourceCode+":"+userToken.getEmail()+" for "+userToken.getRealm());
		
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