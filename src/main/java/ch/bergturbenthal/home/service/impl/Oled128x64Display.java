package ch.bergturbenthal.home.service.impl;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.tinkerforge.BrickletOLED128x64;
import com.tinkerforge.TinkerforgeException;

import ch.bergturbenthal.home.display.Display;
import ch.bergturbenthal.home.display.DisplayContent;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

@Slf4j
public class Oled128x64Display implements Display, Closeable {
    private static final short HEIGHT = 64;

    private static final short WIDTH  = 128;

    private static void drawBuffer(final BrickletOLED128x64 oled, final BufferedImage image) throws TinkerforgeException {
        final short[][] column = new short[HEIGHT / 8][WIDTH];
        final short[] columnWrite = new short[64];
        short page = 0;
        short i, j, k, l;
        // final Map<Integer, AtomicInteger> histogram = new HashMap<>();

        for (i = 0; i < HEIGHT / 8; i++) {
            for (j = 0; j < WIDTH; j++) {
                page = 0;

                for (k = 0; k < 8; k++) {
                    // final int color = image.getRGB(j, (i * 8) + k);
                    // final int r = (color >> 16) & 0xff;
                    // final int g = (color >> 8) & 0xff;
                    // final int b = color & 0xff;
                    // histogram.computeIfAbsent(r + g + b, k2 -> new AtomicInteger()).incrementAndGet();
                    // if (r + g + b > 128 * 3) {
                    if ((image.getRGB(j, (i * 8) + k) & 0x00FFFFFF) > 0) {
                        page |= (short) (1 << k);
                    }
                }

                column[i][j] = page;
            }
        }
        // log.info("Histogram: " + histogram);
        oled.newWindow((short) 0, (short) (WIDTH - 1), (short) 0, (short) 7);

        for (i = 0; i < HEIGHT / 8; i++) {
            l = 0;
            for (j = 0; j < WIDTH / 2; j++) {
                columnWrite[l] = column[i][j];
                l++;
            }

            oled.write(columnWrite);

            l = 0;
            for (k = WIDTH / 2; k < WIDTH; k++) {
                columnWrite[l] = column[i][k];
                l++;
            }

            oled.write(columnWrite);
        }
    }

    private final BrickletOLED128x64          bricklet;
    private final AtomicReference<Disposable> pendingDisposable    = new AtomicReference<Disposable>(null);
    private final Consumer<Disposable>        subscriptionConsumer = t -> {
                                                                       final Disposable oldDisposable = pendingDisposable.getAndSet(t);
                                                                       if (oldDisposable != null) {
                                                                           oldDisposable.dispose();
                                                                       }
                                                                   };

    public Oled128x64Display(final BrickletOLED128x64 bricklet) {
        this.bricklet = bricklet;
    }

    @Override
    public void close() {
        subscriptionConsumer.accept(null);
    }

    @Override
    public void setDisplayContent(final Flux<DisplayContent> content) {
        subscriptionConsumer.accept(content.subscribe(c -> {
            try {
                final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_BINARY);
                final Graphics g = image.createGraphics();
                c.render((Graphics2D) g, WIDTH, HEIGHT);
                g.dispose();
                drawBuffer(bricklet, image);
            } catch (final TinkerforgeException e) {
                log.error("Cannot write oled", e);
            }
        }));
    }
}
