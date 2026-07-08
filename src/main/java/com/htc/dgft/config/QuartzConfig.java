package com.htc.dgft.config;

import com.htc.dgft.scheduler.DgftOrmPushJob;
import com.htc.dgft.scheduler.DgftApiPushJob;
import com.htc.dgft.scheduler.DgftEnquiryJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    // batch staging for csv records
    @Bean
    public JobDetail pushJobDetail() {
        return JobBuilder.newJob(DgftOrmPushJob.class)
                .withIdentity("dgftOrmPushJob", "dgftGroup")
                .storeDurably()
                .build();
    }

    // Trigger for batch staging
    @Bean
    public Trigger pushJobTrigger(JobDetail pushJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(pushJobDetail)
                .withIdentity("dgftOrmPushTrigger", "dgftGroup")
                .withSchedule(CronScheduleBuilder.cronSchedule("0/30 * * * * ?"))
                .build();
    }

    // API push for csv records
    @Bean
    public JobDetail apiPushJobDetail() {
        return JobBuilder.newJob(DgftApiPushJob.class)
                .withIdentity("dgftApiPushJob", "dgftGroup")
                .storeDurably()
                .build();
    }

    // Trigger for API push
    @Bean
    public Trigger apiPushTrigger(JobDetail apiPushJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(apiPushJobDetail)
                .withIdentity("dgftApiPushTrigger", "dgftGroup")
                .withSchedule(CronScheduleBuilder.cronSchedule("0/30 * * * * ?"))
                .build();
    }

    // Enquiry job for CSV records (Instruction 4)
    @Bean
    public JobDetail enquiryJobDetail() {
        return JobBuilder.newJob(DgftEnquiryJob.class)
                .withIdentity("dgftEnquiryJob", "dgftGroup")
                .storeDurably()
                .build();
    }

    // Trigger for Enquiry job - runs daily at 1:00 AM
    @Bean
    public Trigger enquiryJobTrigger(JobDetail enquiryJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(enquiryJobDetail)
                .withIdentity("dgftEnquiryTrigger", "dgftGroup")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 1 * * ?")) // Daily at 1:00 AM
                .build();
    }
}