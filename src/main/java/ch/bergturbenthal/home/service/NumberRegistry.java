package ch.bergturbenthal.home.service;

import java.util.Collection;

import reactor.core.publisher.Flux;

public interface NumberRegistry {
    public static interface Key {
    }

    void addValue(Key key, Flux<Number> value);

    Flux<Number> getValue(Key key);

    Flux<Collection<Key>> listValues();
}
