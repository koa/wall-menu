package ch.bergturbenthal.home.display;

import lombok.Value;

@Value
public class RotateEvent implements InputEvent {
    private int increment;
}
