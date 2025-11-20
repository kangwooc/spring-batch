package com.system.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.converter.JsonJobParametersConverter;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
public class SystemTerminationConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final AtomicInteger processesKilled = new AtomicInteger(0);
    private final int TERMINATION_TARGET = 5;

    public SystemTerminationConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    // json 형식의 잡 파라미터 변환기를 빈으로 등록
    public JobParametersConverter jobParametersConverter() {
        return new JsonJobParametersConverter();
    }

    @Bean
    public Job systemTerminationSimulationJob() {
        return new JobBuilder("systemTerminationSimulationJob", jobRepository)
                .start(enterWorldStep())
                .next(meetNPCStep())
                .next(defeatProcessStep())
                .next(completeQuestStep())
                .build();
    }

    @Bean
    public Step enterWorldStep() {
        return new StepBuilder("enterWorldStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("System Termination 시뮬레이션 세계에 접속했습니다!");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step meetNPCStep() {
        return new StepBuilder("meetNPCStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("시스템 관리자 NPC를 만났습니다.");
                    System.out.println("첫 번째 미션: 좀비 프로세스 " + TERMINATION_TARGET + "개 처형하기");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step defeatProcessStep() {
        return new StepBuilder("defeatProcessStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    int terminated = processesKilled.incrementAndGet();
                    System.out.println("좀비 프로세스 처형 완료! (현재 " + terminated + "/" + TERMINATION_TARGET + ")");
                    if (terminated < TERMINATION_TARGET) {
                        return RepeatStatus.CONTINUABLE;
                    } else {
                        return RepeatStatus.FINISHED;
                    }
                }, transactionManager)
                .build();
    }

    @Bean
    public Step completeQuestStep() {
        return new StepBuilder("completeQuestStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("미션 완료! 좀비 프로세스 " + TERMINATION_TARGET + "개 처형 성공!");
                    System.out.println("보상: kill -9 권한 획득, 시스템 제어 레벨 1 달성");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Job processTerminatorJob(JobRepository jobRepository, Step terminationStep) {
        return new JobBuilder("processTerminatorJob", jobRepository)
                .start(terminationStep)
                .build();
    }

    @Bean
    public Step terminationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet terminatorTasklet) {
        return new StepBuilder("terminationStep", jobRepository)
                .tasklet(terminatorTasklet, transactionManager)
                .build();
    }

    @Bean
    // @Value를 사용해 잡 파라미터를 전달받으려면 @StepScope와 같은 특별한 어노테이션을 선언
    @StepScope
    public Tasklet terminatorTasklet(
            @Value("#{jobParameters['terminatorId']}") String terminatorId,
            @Value("#{jobParameters['targetCount']}") Integer targetCount,
            @Value("#{jobParameters['executionDate']}") LocalDate executionDate,
            @Value("#{jobParameters['startTime']}") LocalDateTime startTime
    ) {
        return (contribution, chunkContext) -> {
            log.info("시스템 종결자 정보:");
            log.info("ID: {}", terminatorId);
            log.info("제거 대상 수: {}", targetCount);
            log.info("⚡ SYSTEM TERMINATOR {} 작전을 개시합니다.", terminatorId);
            log.info("☠️ {}개의 프로세스를 종료합니다.", targetCount);
            log.info("처형 예정일: {}", executionDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
            log.info("작전 개시 시각: {}", startTime.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초")));

            for (int i = 1; i <= targetCount; i++) {
                log.info("💀 프로세스 {} 종료 완료!", i);
            }


            log.info("🎯 임무 완료: 모든 대상 프로세스가 종료되었습니다.");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Job enumTerminatorJob(JobRepository jobRepository, Step enumTerminationStep) {
        return new JobBuilder("enumTerminatorJob", jobRepository)
                .start(enumTerminationStep)
                .build();
    }

    @Bean
    public Step enumTerminationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet enumTerminatorTasklet) {
        return new StepBuilder("enumTerminationStep", jobRepository)
                .tasklet(enumTerminatorTasklet, transactionManager)
                .build();
    }


    @Bean
    @StepScope
    public Tasklet enumTerminatorTasklet(
            @Value("#{jobParameters['questDifficulty']}") QuestDifficulty questDifficulty
    ) {
        return (contribution, chunkContext) -> {
            log.info("⚔️ 시스템 침투 작전 개시!");
            log.info("임무 난이도: {}", questDifficulty);
            // 난이도에 따른 보상 계산
            int baseReward = 100;
            int rewardMultiplier = switch (questDifficulty) {
                case EASY -> 1;
                case NORMAL -> 2;
                case HARD -> 3;
                case EXTREME -> 5;
            };
            int totalReward = baseReward * rewardMultiplier;
            log.info("💥 시스템 해킹 진행 중...");
            log.info("🏆 시스템 장악 완료!");
            log.info("💰 획득한 시스템 리소스: {} 메가바이트", totalReward);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Job pojoTerminatorJob(JobRepository jobRepository, Step pojoTerminationStep) {
        return new JobBuilder("pojoTerminatorJob", jobRepository)
                .start(pojoTerminationStep)
                .build();
    }

    @Bean
    public Step pojoTerminationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet pojoTerminatorTasklet) {
        return new StepBuilder("pojoTerminationStep", jobRepository)
                .tasklet(pojoTerminatorTasklet, transactionManager)
                .build();
    }

    @Bean
    public Tasklet pojoTerminatorTasklet(SystemInfiltrationParameters infiltrationParams) {
        return (contribution, chunkContext) -> {
            log.info("⚔️ 시스템 침투 작전 초기화!");
            log.info("임무 코드네임: {}", infiltrationParams.getMissionName());
            log.info("보안 레벨: {}", infiltrationParams.getSecurityLevel());
            log.info("작전 지휘관: {}", infiltrationParams.getOperationCommander());

            // 보안 레벨에 따른 침투 난이도 계산
            int baseInfiltrationTime = 60; // 기본 침투 시간 (분)
            int infiltrationMultiplier = switch (infiltrationParams.getSecurityLevel()) {
                case 1 -> 1; // 저보안
                case 2 -> 2; // 중보안
                case 3 -> 4; // 고보안
                case 4 -> 8; // 최고 보안
                default -> 1;
            };

            int totalInfiltrationTime = baseInfiltrationTime * infiltrationMultiplier;

            log.info("💥 시스템 해킹 난이도 분석 중...");
            log.info("🕒 예상 침투 시간: {}분", totalInfiltrationTime);
            log.info("🏆 시스템 장악 준비 완료!");

            return RepeatStatus.FINISHED;
        };
    }


    @Bean
    public Job pojoJsonTerminatorJob(JobRepository jobRepository, Step pojoJsonTerminationStep) {
        return new JobBuilder("pojoJsonTerminatorJob", jobRepository)
                .start(pojoJsonTerminationStep)
                .build();
    }

    @Bean
    public Step pojoJsonTerminationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet pojoJsonTerminatorTasklet) {
        return new StepBuilder("pojoJsonTerminationStep", jobRepository)
                .tasklet(pojoJsonTerminatorTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet pojoJsonTerminatorTasklet(@Value("#{jobParameters['infiltrationTargets']}") String infiltrationTargets) {
        return (contribution, chunkContext) -> {
            String[] targets = infiltrationTargets.split(",");

            log.info("⚡ 침투 작전 개시");
            log.info("첫 번째 타겟: {} 침투 시작", targets[0]);
            log.info("마지막 타겟: {} 에서 집결", targets[1]);
            log.info("🎯 임무 전달 완료");

            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    @JobScope
    public Tasklet systemDestructionTasklet(
            @Value("#{jobExecutionContext['previousSystemState']}") String prevState
    ) {
        // JobExecution의 ExecutionContext에서 이전 시스템 상태를 주입받는다

        return (contribution, chunkContext) -> {
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    @StepScope
    public Tasklet infiltrationTasklet(
            @Value("#{stepExecutionContext['targetSystemStatus']}") String targetStatus
    ) {
        // StepExecution의 ExecutionContext에서 타겟 시스템 상태를 주입받는다
    }
}
