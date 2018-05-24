package ch.bergturbenthal.home.service.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import ch.bergturbenthal.home.model.event.AddTerminalEvent;
import ch.bergturbenthal.home.model.event.RemoveTerminalEvent;
import ch.bergturbenthal.home.model.event.StoredEvent;
import ch.bergturbenthal.home.model.event.UpdateTerminalDataEvent;
import ch.bergturbenthal.home.model.event.thing.TerminalData;
import ch.bergturbenthal.home.model.event.thing.TerminalId;
import ch.bergturbenthal.home.service.StoredEventConsumer;
import lombok.Builder;
import lombok.Value;

@Service
public class TerminalEventConsumer implements StoredEventConsumer {

    @Value
    @Builder(toBuilder = true)
    private static class TerminalState {
        private TerminalData data;
    }

    private final Map<TerminalId, TerminalState> terminals = new ConcurrentHashMap<TerminalId, TerminalState>();

    @Override
    public void accept(final StoredEvent event) {
        if (event instanceof UpdateTerminalDataEvent) {
            final UpdateTerminalDataEvent updateTerminalDataEvent = (UpdateTerminalDataEvent) event;
            terminals.computeIfPresent(updateTerminalDataEvent.getId(),
                    (id, oldState) -> oldState.toBuilder().data(updateTerminalDataEvent.getData()).build());
        } else if (event instanceof RemoveTerminalEvent) {
            terminals.remove(((RemoveTerminalEvent) event).getId());
        } else if (event instanceof AddTerminalEvent) {
            terminals.put(((AddTerminalEvent) event).getId(), TerminalState.builder().build());
        }

    }

}
