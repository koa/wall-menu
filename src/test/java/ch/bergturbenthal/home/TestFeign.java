package ch.bergturbenthal.home;

import java.time.Duration;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ch.bergturbenthal.home.model.settings.WallMenuProperties;
import ch.bergturbenthal.home.openhab.api.UuidApiClient;

//@Import({ ClientConfiguration.class, SpringSecurityConfig.class })
@Import(WallMenuConfiguration.class)
@SpringBootApplication
@EnableWebMvc
public class TestFeign {
    public static void main(final String[] args) throws InterruptedException {
        final ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder(TestFeign.class).web(WebApplicationType.SERVLET)
                .build().run(args);
        final UuidApiClient uuidApiClient = applicationContext.getBean(UuidApiClient.class);
        final ResponseEntity<String> uuid = uuidApiClient.getInstanceUUID();
        System.out.println(uuid);
        Thread.sleep(Duration.ofMinutes(5).toMillis());
        applicationContext.close();
    }

    @Bean
    public WallMenuProperties wallMenuProperties() {
        return new WallMenuProperties();
    }

}
