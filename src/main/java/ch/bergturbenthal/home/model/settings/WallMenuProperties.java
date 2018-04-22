package ch.bergturbenthal.home.model.settings;

import java.io.File;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "wallmenu")
public class WallMenuProperties {
    private File storedir = new File("/tmp");
}
