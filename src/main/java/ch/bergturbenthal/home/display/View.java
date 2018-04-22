package ch.bergturbenthal.home.display;

import reactor.core.publisher.Flux;

public interface View {
    void addInputEvents(Flux<InputEvent> eventStream);

    Flux<ShowEvent> getDisplayStream();
}
