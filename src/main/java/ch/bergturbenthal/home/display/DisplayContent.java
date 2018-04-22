package ch.bergturbenthal.home.display;

import java.awt.Graphics2D;

public interface DisplayContent extends ShowEvent {
    void render(Graphics2D graphics, int width, int height);
}