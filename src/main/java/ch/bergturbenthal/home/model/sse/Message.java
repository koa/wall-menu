package ch.bergturbenthal.home.model.sse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import lombok.Value;

@Value
public class Message {
    private static final Map<String, ObjectReader> payloadDecoders;
    static {
        payloadDecoders = new HashMap<>();
        final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        payloadDecoders.put("ItemStateEvent", objectMapper.readerFor(ItemStateEvent.class));
        payloadDecoders.put("ItemStateChangedEvent", objectMapper.readerFor(ItemStateChangedEvent.class));
        payloadDecoders.put("ItemCommandEvent", objectMapper.readerFor(ItemCommandEvent.class));
    }
    private String topic;
    private String payload;
    private String type;

    public Payload extractPayload() {
        try {
            return payloadDecoders.get(type).readValue(payload);
        } catch (final IOException e) {
            throw new RuntimeException("Cannot decode type " + type + ": " + payload, e);
        }
    }
}
