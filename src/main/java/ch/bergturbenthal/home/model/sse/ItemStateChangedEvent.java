package ch.bergturbenthal.home.model.sse;

import lombok.Value;

@Value
public class ItemStateChangedEvent implements Payload {
    private String type;
    private String value;
    private String oldType;
    private String oldValue;

}
