package ch.bergturbenthal.home.model.event;

import java.time.Instant;

import ch.bergturbenthal.home.model.event.thing.TerminalId;
import lombok.Value;

@Value
public class AddTerminalEvent implements StoredEvent {
    private Instant    timestamp;
    private TerminalId id;
}
