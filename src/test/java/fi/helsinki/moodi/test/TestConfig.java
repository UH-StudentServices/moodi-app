package fi.helsinki.moodi.test;

import fi.helsinki.moodi.service.time.SystemTimeService;
import fi.helsinki.moodi.service.time.TimeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class TestConfig {

    @Bean
    public TimeService timeService() {
        return new SystemTimeService() {

            @Override
            public LocalDateTime getCurrentDateTime() {
                return LocalDateTime.of(2015, 5, 18, 10, 15, 0);
            }
        };
    }
}
