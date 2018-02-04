package ch.bergturbenthal.home;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;

import ch.bergturbenthal.home.openhab.api.UuidApiClient;
import ch.bergturbenthal.home.openhab.config.ClientConfiguration;

@Import(ClientConfiguration.class)
@SpringBootApplication
public class TestFeign {
    public static void main(final String[] args) {
        final ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder(TestFeign.class).web(WebApplicationType.NONE)
                .run(args);
        final UuidApiClient uuidApiClient = applicationContext.getBean(UuidApiClient.class);
        final ResponseEntity<String> uuid = uuidApiClient.getInstanceUUID();
        System.out.println(uuid);
    }

}
