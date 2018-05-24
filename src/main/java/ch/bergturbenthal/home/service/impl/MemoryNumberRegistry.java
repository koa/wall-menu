package ch.bergturbenthal.home.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Service;

import ch.bergturbenthal.home.service.NumberRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

@Service
public class MemoryNumberRegistry implements NumberRegistry {

    private final Map<Key, Flux<Number>>           knownNumbers       = new ConcurrentHashMap<>();
    private final Queue<FluxSink<Collection<Key>>> keyUpdateListeners = new ConcurrentLinkedQueue<>();

    @Override
    public void addValue(final Key key, final Flux<Number> value) {
        final Flux<Number> valueFlux = value.doOnComplete(() -> knownNumbers.remove(key)).cache(1);
        knownNumbers.compute(key, (k, old) -> {
            if (old != null) {
                throw new IllegalArgumentException("Key " + key + " is already registered");
            }
            return valueFlux;
        });
        final Set<Key> keys = Collections.unmodifiableSet(new HashSet<>(knownNumbers.keySet()));
        for (final FluxSink<Collection<Key>> fluxSink : keyUpdateListeners) {
            fluxSink.next(keys);
        }
    }

    @Override
    public Flux<Number> getValue(final Key key) {
        return knownNumbers.get(key);
    }

    @Override
    public Flux<Collection<Key>> listValues() {
        return Flux.create(sink -> {
            keyUpdateListeners.add(sink);
            sink.onDispose(() -> keyUpdateListeners.remove(sink));
        });
    }

}
