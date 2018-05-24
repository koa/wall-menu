package ch.bergturbenthal.home.model.event;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(AddLightEvent.class),
        @JsonSubTypes.Type(RemoveLightEvent.class),
        @JsonSubTypes.Type(UpdateLightDataEvent.class),
        @JsonSubTypes.Type(AddTerminalEvent.class),
        @JsonSubTypes.Type(RemoveTerminalEvent.class),
        @JsonSubTypes.Type(UpdateTerminalDataEvent.class)

})
public interface StoredEvent {
    Instant getTimestamp();
}
