package ch.bergturbenthal.home.display;

import reactor.core.publisher.Flux;

public interface Display {
    void setDisplayContent(Flux<DisplayContent> content);
}
