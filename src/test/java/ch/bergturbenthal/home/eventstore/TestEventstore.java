package ch.bergturbenthal.home.eventstore;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import ch.bergturbenthal.home.model.event.AddLightEvent;
import ch.bergturbenthal.home.model.event.StoredEvent;
import ch.bergturbenthal.home.model.event.thing.LightId;
import ch.bergturbenthal.home.model.settings.WallMenuProperties;
import ch.bergturbenthal.home.service.StoreService;
import ch.bergturbenthal.home.service.StoredEventConsumer;
import ch.bergturbenthal.home.service.impl.FileEventStore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestEventstore {
    private void cleanup(final WallMenuProperties properties) throws IOException {
        FileUtils.deleteDirectory(properties.getStoredir());
    }

    private WallMenuProperties initProperties() throws IOException {
        final WallMenuProperties properties = new WallMenuProperties();
        final File tempDir = File.createTempFile("events", "");
        tempDir.delete();
        properties.setStoredir(tempDir);
        return properties;
    }

    @Test
    public void testCompletionAndSerialization() throws Throwable {
        final WallMenuProperties properties = initProperties();
        final Queue<Throwable> errors = new ConcurrentLinkedQueue<>();
        final List<StoredEventConsumer> listeners = Arrays.asList(new StoredEventConsumer() {
            private long lastCounter = -1;

            @Override
            public void accept(final StoredEvent storedEvent) {
                final AddLightEvent event = (AddLightEvent) storedEvent;
                final long c = event.getId().getId().getLeastSignificantBits();
                log.info("Id: " + c);
                try {
                    Assert.assertEquals("Jump in ids", lastCounter + 1, c);
                    // if (lastCounter + 1 != c) {
                    // log.error("jump " + lastCounter + " -> " + c);
                    // }
                } catch (final Throwable ex) {
                    errors.add(ex);
                }
                lastCounter = c;
            }

            @Override
            public void init(final StoreService storeService) {
                storeService.storeEvent(new AddLightEvent(Instant.now(), new LightId(new UUID(2, 0))));
            }
        });
        final FileEventStore fileEventStore = new FileEventStore(properties, listeners);
        final Thread thread = new Thread(() -> {
            try {
                for (int i = 1; i < 1000; i++) {
                    final AddLightEvent event = new AddLightEvent(Instant.now(), new LightId(new UUID(0, i)));
                    fileEventStore.storeEvent(event);
                    Thread.sleep(1);
                }
            } catch (final InterruptedException e) {
                log.error("Error sending events", e);
            }
        });
        thread.start();
        Thread.sleep(500);
        thread.join();
        cleanup(properties);
        for (final Throwable exception : errors) {
            throw exception;
        }
    }

    @Test
    public void testStoreEvents() throws IOException {
        final WallMenuProperties properties = initProperties();
        {
            final FileEventStore fileEventStore = new FileEventStore(properties, Collections.emptyList());
            final AddLightEvent event = new AddLightEvent(Instant.now(), new LightId(UUID.randomUUID()));
            fileEventStore.storeEvent(event);
            // final List<StoredEvent> storedEvents = fileEventStore.readEvents(false).collectList().block();
            // Assert.assertEquals(1, storedEvents.size());
            // Assert.assertEquals(event, storedEvents.get(0));
            // log.info("Stored events: " + storedEvents);
        }
        cleanup(properties);
    }

    // @Test
    // public void testStoreLiveEvents() throws Exception {
    // final WallMenuProperties properties = initProperties();
    // final FileEventStore fileEventStore = new FileEventStore(properties);
    // final AddLightEvent event1 = new AddLightEvent(Instant.now(), new LightId(UUID.randomUUID()));
    // fileEventStore.storeEvent(event1);
    // final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(3);
    // final Semaphore sem = new Semaphore(0);
    // fileEventStore.readEvents(true).subscribe(queue::add, queue::add, sem::release);
    // final RemoveLightEvent event2 = new RemoveLightEvent(Instant.now(), event1.getId());
    // fileEventStore.storeEvent(event2);
    // fileEventStore.close();
    // sem.acquire();
    // while (true) {
    // final Object result = queue.poll(2, TimeUnit.SECONDS);
    // if (result == null || result instanceof TimeoutException) {
    // break;
    // }
    // if (result instanceof Exception) {
    // throw (Exception) result;
    // }
    // if (!(result instanceof StoredEvent)) {
    // throw new AssertionError("Wrong result type " + result);
    // }
    // final StoredEvent storedEvents = (StoredEvent) result;
    // System.out.println(storedEvents);
    // }
    // }
}
