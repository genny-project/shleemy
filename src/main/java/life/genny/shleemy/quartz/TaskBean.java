package life.genny.shleemy.quartz;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class TaskBean {

    @Inject
    org.quartz.Scheduler quartz; 

    void onStart(@Observes StartupEvent event) throws SchedulerException {
       JobDetail job = JobBuilder.newJob(MyJob.class)
                         .withIdentity("myJob", "myGroup")
                         .build();
       Trigger trigger = TriggerBuilder.newTrigger()
                            .withIdentity("myTrigger", "myGroup")
                            .startNow()
                            .withSchedule(
                               SimpleScheduleBuilder.simpleSchedule()
                                  .withIntervalInSeconds(10)
                                  .repeatForever())
                            .build();
       quartz.scheduleJob(job, trigger); 
    }

    @Transactional
    void performTask() {
        Task task = new Task();
        task.persist();
    }

    // A new instance of MyJob is created by Quartz for every job execution
    public static class MyJob implements Job {

       @Inject
       TaskBean taskBean;

       public void execute(JobExecutionContext context) throws JobExecutionException {
          taskBean.performTask(); 
       }

    }
}