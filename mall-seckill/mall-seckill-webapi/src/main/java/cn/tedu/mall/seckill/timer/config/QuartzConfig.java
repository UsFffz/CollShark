package cn.tedu.mall.seckill.timer.config;

import cn.tedu.mall.seckill.timer.job.SeckillBloomInitialJob;
import cn.tedu.mall.seckill.timer.job.SeckillInitialJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//向spring容器中保存jobdetail
public class QuartzConfig {
    @Bean
    public JobDetail initJobDetail(){
        return JobBuilder.newJob(SeckillInitialJob.class)
                .withIdentity("initSeckill")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger triggerSeckill(){
        CronScheduleBuilder cronScheduleBuilder
                //实际开发要写出正确的扩容表达式 让程序在11.55 13.55 .... 运行
                = CronScheduleBuilder.cronSchedule("0 55 11,13,15,17,19,21,23 * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(initJobDetail())
                .withIdentity("newB")
                .withSchedule(cronScheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail seckillBloomJobDetail(){
        return JobBuilder.newJob(SeckillBloomInitialJob.class)
                         .withIdentity("seckillBloom")
                         .storeDurably()
                         .build();
    }

    @Bean
    public Trigger seckillBloomTrigger(){
        return TriggerBuilder.newTrigger()
                             .forJob(seckillBloomJobDetail())
                             .withIdentity("seckillBloomTrigger")
                             .withSchedule(CronScheduleBuilder.cronSchedule("0/30 * * * * ?"))
                             .build();
    }
}
