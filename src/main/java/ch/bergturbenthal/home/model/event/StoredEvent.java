package ch.bergturbenthal.home.model.event;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(AddLightEvent.class),
        @JsonSubTypes.Type(RemoveLightEvent.class),
        @JsonSubTypes.Type(UpdateLightData.class),
        @JsonSubTypes.Type(AddLightEvent.class) })
public interface StoredEvent {
    Instant getTimestamp();
}
