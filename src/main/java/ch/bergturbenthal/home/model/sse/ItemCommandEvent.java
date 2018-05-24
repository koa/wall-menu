package ch.bergturbenthal.home.model.sse;

import lombok.Value;

@Value
public class ItemCommandEvent implements Payload {
    private String type;
    private String value;
}
