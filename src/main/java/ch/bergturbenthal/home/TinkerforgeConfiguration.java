package ch.bergturbenthal.home;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class TinkerforgeConfiguration {
    @Bean
    public TinkerforgeProperties tinkerforgeProperties() {
        return new TinkerforgeProperties();
    }
}
