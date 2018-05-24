package ch.bergturbenthal.home.model.event;

import java.time.Instant;

import ch.bergturbenthal.home.model.event.thing.TerminalData;
import ch.bergturbenthal.home.model.event.thing.TerminalId;
import lombok.Value;

@Value
public class UpdateTerminalDataEvent implements StoredEvent {
    private Instant      timestamp;
    private TerminalId   id;
    private TerminalData data;

}
