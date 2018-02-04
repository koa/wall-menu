package ch.bergturbenthal.home.openhab.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

import ch.bergturbenthal.home.openhab.api.UuidApiClient;

@Configuration
@EnableConfigurationProperties
@EnableFeignClients(basePackageClasses = UuidApiClient.class)
public class ClientConfiguration {

}
