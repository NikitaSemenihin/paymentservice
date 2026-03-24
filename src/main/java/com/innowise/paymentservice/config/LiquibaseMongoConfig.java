package com.innowise.paymentservice.config;

import liquibase.command.CommandScope;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.Scope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiquibaseMongoConfig {

    @Bean
    ApplicationRunner liquibaseRunner(
            @Value("${spring.data.mongodb.uri}")
            String mongoUri,
            @Value("${app.liquibase.change-log}")
            String changeLog
    ) {
        return args -> {
            String normalizedChangeLog = normalizeChangeLog(changeLog);
            Scope.child(Scope.Attr.resourceAccessor, new ClassLoaderResourceAccessor(), () -> {
                CommandScope update = new CommandScope("update");
                update.addArgumentValue("url", mongoUri);
                update.addArgumentValue("changeLogFile", normalizedChangeLog);
                update.execute();
            });
        };
    }

    private String normalizeChangeLog(String changeLog) {
        if (changeLog != null && changeLog.startsWith("classpath:")) {
            return changeLog.substring("classpath:".length());
        }
        return changeLog;
    }
}
