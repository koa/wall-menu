package ch.bergturbenthal.home.model.event.thing;

import lombok.Value;

@Value
public class LightData {
    private LightType type;
    private String    name;
}
