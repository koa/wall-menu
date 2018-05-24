package ch.bergturbenthal.home.model.event.thing;

import lombok.Value;

@Value
public class TerminalData {
    private TerminalType type;
    private String       name;
}
