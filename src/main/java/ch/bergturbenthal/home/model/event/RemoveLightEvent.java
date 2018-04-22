package ch.bergturbenthal.home.model.event;

import java.time.Instant;

import ch.bergturbenthal.home.model.event.thing.LightId;
import lombok.Value;

@Value
public class RemoveLightEvent implements StoredEvent {
    private Instant timestamp;
    private LightId id;
}