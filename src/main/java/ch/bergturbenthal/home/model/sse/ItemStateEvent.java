package ch.bergturbenthal.home.model.sse;

import lombok.Value;

@Value
public class ItemStateEvent implements Payload {
    private String type;
    private String value;
}
