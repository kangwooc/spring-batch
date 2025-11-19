package com.system.batch.tasklet;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ZombieBatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public ZombieBatchConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public Tasklet zombieProcessCleanupTasklet() {
        return new ZombieProcessCleanupTasklet();
    }

    // StepBuilder의 tasklet() 메서드를 호출하면, 스텝 빌더는 태스크릿 지향 처리 방식의 Step을 생성
    @Bean
    public Step zombieCleanupStep() {
        return new StepBuilder("zombieCleanupStep", jobRepository)
//                .tasklet(zombieProcessCleanupTasklet(), transactionManager) // Tasklet과 transactionManager 설정
                .tasklet(zombieProcessCleanupTasklet(), new ResourcelessTransactionManager()) // 트랜잭션 매니저를 ResourcelessTransactionManager로 설정
                .build();
    }

    @Bean
    public Job zombieProcessCleanupJob() {
        return new JobBuilder("zombieProcessCleanupJob", jobRepository)
                .start(zombieCleanupStep())  // Step 등록
                .build();
    }
}
