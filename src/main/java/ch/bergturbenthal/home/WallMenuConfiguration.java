package ch.bergturbenthal.home;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import ch.bergturbenthal.home.model.settings.TinkerforgeProperties;
import ch.bergturbenthal.home.model.settings.WallMenuProperties;

@Configuration
@EnableScheduling
@EnableAutoConfiguration
public class WallMenuConfiguration {
    @Bean
    public TinkerforgeProperties tinkerforgeProperties() {
        return new TinkerforgeProperties();
    }

    @Bean
    public WallMenuProperties wallMenuProperties() {
        return new WallMenuProperties();
    }
}
