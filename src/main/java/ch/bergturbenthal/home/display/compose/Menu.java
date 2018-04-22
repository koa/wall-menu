package ch.bergturbenthal.home.display.compose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import ch.bergturbenthal.home.display.InputEvent;
import ch.bergturbenthal.home.display.ShowEvent;
import ch.bergturbenthal.home.display.View;
import lombok.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

public class Menu implements View {
    @Value
    private static class ExternalInputEvent implements MenuInputEvent {
        private InputEvent event;
    }

    @Value
    private static class MenuEntry {
        private String title;
        private View   content;
    }

    private static interface MenuInputEvent {
    }

    @Value
    private static class RefreshMenuEvent implements MenuInputEvent {
        private List<MenuEntry> menuContent;
    }

    private final Consumer<MenuEntry> addMenuEntryConsumer;
    private Flux<MenuInputEvent>      updateStream;

    public Menu() {
        final AtomicReference<List<MenuEntry>> menuContent = new AtomicReference<>(Collections.emptyList());
        final AtomicReference<Consumer<MenuInputEvent>> globalEventConsumer = new AtomicReference<Consumer<MenuInputEvent>>(event -> {
        });
        updateStream = Flux.create((final FluxSink<MenuInputEvent> sink) -> {
            globalEventConsumer.set(event -> sink.next(event));
        }).cache(1);
        addMenuEntryConsumer = menuEntry -> {
            final List<MenuEntry> updatedMenu = menuContent.updateAndGet(m -> {
                final List<MenuEntry> newMenu = new ArrayList<>(m);
                newMenu.add(menuEntry);
                return Collections.unmodifiableList(newMenu);
            });
            globalEventConsumer.get().accept(new RefreshMenuEvent(updatedMenu));
        };
    }

    @Override
    public void addInputEvents(final Flux<InputEvent> eventStream) {
        // TODO Auto-generated method stub

    }

    public void addMenuEntry(final String title, final View content) {
        addMenuEntryConsumer.accept(new MenuEntry(title, content));
    }

    @Override
    public Flux<ShowEvent> getDisplayStream() {
        // TODO Auto-generated method stub
        return null;
    }

    // @Override
    // public Flux<ShowEvent> getDisplayStream(final Flux<InputEvent> eventStream) {
    // return Flux.create(sink -> {
    // final AtomicInteger currentMenuEntry = new AtomicInteger(0);
    // final AtomicInteger topMenuEntry = new AtomicInteger(0);
    // final Flux<MenuInputEvent> inputEventStream = eventStream.map(e -> (MenuInputEvent) new ExternalInputEvent(e)).mergeWith(updateStream);
    // final Disposable subscription = inputEventStream.subscribe(event -> {
    // });
    // sink.onDispose(() -> subscription.dispose());
    //
    // });
    // }
}
