package ch.bergturbenthal.home.model.event;

import java.time.Instant;

import ch.bergturbenthal.home.model.event.thing.LightData;
import ch.bergturbenthal.home.model.event.thing.LightId;
import lombok.Value;

@Value
public class UpdateLightDataEvent implements StoredEvent {
    private Instant   timestamp;
    private LightId   id;
    private LightData data;

}
