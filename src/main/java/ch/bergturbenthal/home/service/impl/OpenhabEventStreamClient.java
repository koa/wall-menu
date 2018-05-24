package ch.bergturbenthal.home.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import ch.bergturbenthal.home.model.sse.Message;
import ch.bergturbenthal.home.openhab.api.ItemsApiClient;
import ch.bergturbenthal.home.openhab.model.EnrichedItemDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OpenhabEventStreamClient {
    public OpenhabEventStreamClient(@Value("${openHABREST_.url}") final String openhabUrl, final ItemsApiClient itemsApi) {
        // final String eventsUri = new DefaultUriBuilderFactory(openhabUrl).builder().path("events").build().toString();
        final ResponseEntity<List<EnrichedItemDTO>> itemList = itemsApi.getItems(null, null, null, null, null);
        for (final EnrichedItemDTO item : itemList.getBody()) {
            log.info("Item: " + item);
        }
        final ResponseSpec retrieve = WebClient.create(openhabUrl).get().uri("/events").accept(MediaType.TEXT_EVENT_STREAM).retrieve();
        retrieve.bodyToFlux(Message.class).subscribe(value -> {
            log.info("Value: " + value);
            log.info("Payload: " + value.extractPayload());
        }, ex -> {
            log.error("Error", ex);
        }, () -> {
            log.info("Closed");
        });
    }
}
