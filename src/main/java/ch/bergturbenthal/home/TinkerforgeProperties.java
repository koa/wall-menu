package ch.bergturbenthal.home;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "tinkerforge")
@Component
public class TinkerforgeProperties {
    private List<String> discovery;
}
