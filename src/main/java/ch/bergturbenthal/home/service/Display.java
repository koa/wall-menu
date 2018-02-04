package ch.bergturbenthal.home.service;

import java.awt.Graphics2D;

public interface Display {
    interface DisplayRenderer {
        void render(Graphics2D graphics, int width, int height);
    }

    void draw(DisplayRenderer renderer);
}
