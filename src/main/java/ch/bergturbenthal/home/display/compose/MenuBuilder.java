package ch.bergturbenthal.home.display.compose;

import java.util.ArrayList;
import java.util.List;

import ch.bergturbenthal.home.display.InputEvent;
import ch.bergturbenthal.home.display.ShowEvent;
import ch.bergturbenthal.home.display.View;
import lombok.Value;
import reactor.core.publisher.Flux;

public class MenuBuilder {
    @Value
    private static class MenuEntry {
        private String title;
        private View   content;
    }

    List<MenuEntry> entries = new ArrayList<>();

    public MenuBuilder addEntry(final String title, final View content) {
        entries.add(new MenuEntry(title, content));
        return this;
    }

    public View build() {
        return new View() {

            @Override
            public void addInputEvents(final Flux<InputEvent> eventStream) {
                // TODO Auto-generated method stub

            }

            @Override
            public Flux<ShowEvent> getDisplayStream() {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

}
