package com.system.batch.filewrite;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemWriterBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class MultiResourceItemWriteJobConfig {

    @Bean
    public Job deathNoteMultiWriteJob(
            JobRepository jobRepository,
            Step deathNoteMultiWriteStep
    ) {
        return new JobBuilder("deathNoteMultiWriteJob", jobRepository)
                .start(deathNoteMultiWriteStep)
                .build();
    }

    @Bean
    public Step deathNoteMultiWriteStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ListItemReader<DeathNote> deathNoteMultiListReader,
            FlatFileItemWriter<DeathNote> multiResourceItemWriter
    ) {
        return new StepBuilder("deathNoteMultiWriteStep", jobRepository)
                .<DeathNote, DeathNote>chunk(10, transactionManager)
                .reader(deathNoteMultiListReader)
                .writer(multiResourceItemWriter)
                .build();
    }

    @Bean
    public ListItemReader<DeathNote> deathNoteMultiListReader() {
        List<DeathNote> deathNotes = new ArrayList<>();
        for (int i = 1; i <= 15; i++) { // 총 15개의 DeathNote 객체 read()
            String id = String.format("KILL-%03d", i);
            LocalDate date = LocalDate.now().plusDays(i);
            deathNotes.add(new DeathNote(
                    id,
                    "피해자" + i,
                    date.format(DateTimeFormatter.ISO_DATE),
                    "처형사유" + i
            ));
        }
        return new ListItemReader<>(deathNotes);
    }

    @Bean
    @StepScope
    public MultiResourceItemWriter<DeathNote> multiResourceItemWriter(
            @Value("#{jobParameters['outputDir']}") String outputDir) {
        return new MultiResourceItemWriterBuilder<DeathNote>()
                .name("multiDeathNoteWriter")
                .resource(new FileSystemResource(outputDir + "/death_note"))
                .itemCountLimitPerResource(10)
                .delegate(delegateMultiItemWriter())
                .resourceSuffixCreator(index -> String.format("_%03d.txt", index))
                .build();
    }

    @Bean
    public FlatFileItemWriter<DeathNote> delegateMultiItemWriter() {
        return new FlatFileItemWriterBuilder<DeathNote>()
                .name("deathNoteMultiWriter")
                .formatted()
                .format("처형 ID: %s | 처형일자: %s | 피해자: %s | 사인: %s")
                .sourceType(DeathNote.class)
                .names("victimId", "executionDate", "victimName", "causeOfDeath")
                .headerCallback(writer -> writer.write("================= 처형 기록부 ================="))
                .footerCallback(writer -> writer.write("================= 처형 완료 =================="))
                .build();
    }

    public record DeathNote(String victimId, String victimName, String executionDate, String causeOfDeath) {}
}
