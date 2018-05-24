package ch.bergturbenthal.home.service;

import java.util.function.Consumer;

import ch.bergturbenthal.home.model.event.StoredEvent;

public interface StoredEventConsumer extends Consumer<StoredEvent> {
    default void init(final StoreService storeService) {
    };
}
