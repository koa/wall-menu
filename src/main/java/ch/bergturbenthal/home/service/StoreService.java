package ch.bergturbenthal.home.service;

import ch.bergturbenthal.home.model.event.StoredEvent;

public interface StoreService {

    void storeEvent(StoredEvent event);
}
