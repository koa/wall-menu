package ch.bergturbenthal.home.service;

import ch.bergturbenthal.home.model.event.StoredEvent;
import reactor.core.publisher.Flux;

public interface StoreService {
    Flux<StoredEvent> readEvents(boolean live);

    void storeEvent(StoredEvent event);
}
