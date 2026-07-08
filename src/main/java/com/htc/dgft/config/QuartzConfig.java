package com.htc.dgft.config;

import com.htc.dgft.scheduler.DgftOrmPushJob;
import com.htc.dgft.scheduler.DgftApiPushJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

	// Job detail structure for batch staging (Instruction 2)
    @Bean
    public JobDetail pushJobDetail() {
        return JobBuilder.newJob(DgftOrmPushJob.class)
                .withIdentity("dgftOrmPushJob", "dgftGroup")
                .storeDurably()
                .build();
    }

    // Trigger for batch staging (every 30 seconds)
    @Bean
    public Trigger pushJobTrigger(JobDetail pushJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(pushJobDetail)
                .withIdentity("dgftOrmPushTrigger", "dgftGroup")
                .withSchedule(CronScheduleBuilder.cronSchedule("0/30 * * * * ?"))
                .build();
    }

    // Job detail structure for API push (Instruction 3)
    @Bean
    public JobDetail apiPushJobDetail() {
        return JobBuilder.newJob(DgftApiPushJob.class)
                .withIdentity("dgftApiPushJob", "dgftGroup")
                .storeDurably()
                .build();
    }

    // Trigger for API push (every 30 seconds)
    @Bean
    public Trigger apiPushTrigger(JobDetail apiPushJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(apiPushJobDetail)
                .withIdentity("dgftApiPushTrigger", "dgftGroup")
                .withSchedule(CronScheduleBuilder.cronSchedule("0/30 * * * * ?"))
                .build();
    }
}
