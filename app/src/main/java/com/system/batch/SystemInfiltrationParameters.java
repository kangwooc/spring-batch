package com.system.batch;

import lombok.Data;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
// 앞서 설명했듯이 잡 파라미터를 받으려면 @StepScope 와 같은 특별한 애노테이션이 필요하다. 이전 예제에서 배운 내용을 그대로 적용해 이 POJO 빈을 @StepScope로 선언했다.
@StepScope
// @Component 애노테이션으로 Spring 빈으로 등록된다.
@Component
public class SystemInfiltrationParameters {
    @Value("#{jobParameters[missionName]}")
    private String missionName;
    private int securityLevel;
    private final String operationCommander;

    public SystemInfiltrationParameters(@Value("#{jobParameters[operationCommander]}") String operationCommander) {
        this.operationCommander = operationCommander;
    }

    @Value("#{jobParameters[securityLevel]}")
    public void setSecurityLevel(int securityLevel) {
        this.securityLevel = securityLevel;
    }
}
