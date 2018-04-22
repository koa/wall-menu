package ch.bergturbenthal.home.eventstore;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import ch.bergturbenthal.home.model.event.AddLightEvent;
import ch.bergturbenthal.home.model.event.RemoveLightEvent;
import ch.bergturbenthal.home.model.event.StoredEvent;
import ch.bergturbenthal.home.model.event.thing.LightId;
import ch.bergturbenthal.home.model.settings.WallMenuProperties;
import ch.bergturbenthal.home.service.impl.FileEventStore;
import lombok.Cleanup;
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
    public void testCompletionAndSerialization() throws IOException, InterruptedException {
        final WallMenuProperties properties = initProperties();
        final FileEventStore fileEventStore = new FileEventStore(properties);
        new Thread(() -> {
            try {
                for (int i = 0; i < 1000; i++) {
                    final AddLightEvent event = new AddLightEvent(Instant.now(), new LightId(new UUID(0, i)));
                    fileEventStore.storeEvent(event);
                    Thread.sleep(1);
                }
                fileEventStore.close();
            } catch (final InterruptedException | IOException e) {
                log.error("Error sending events", e);
            }
        }).start();
        Thread.sleep(500);
        final List<StoredEvent> events = fileEventStore.readEvents(true).collectList().block(Duration.ofSeconds(20));
        cleanup(properties);
        Assert.assertEquals(1000, events.size());
        long lastCounter = -1;
        for (final StoredEvent storedEvent : events) {
            final AddLightEvent event = (AddLightEvent) storedEvent;
            final long c = event.getId().getId().getLeastSignificantBits();
            Assert.assertEquals("Jump in ids", lastCounter + 1, c);
            // if (lastCounter + 1 != c) {
            // log.error("jump " + lastCounter + " -> " + c);
            // }
            lastCounter = c;
        }
    }

    @Test
    public void testStoreEvents() throws IOException {
        final WallMenuProperties properties = initProperties();
        {
            @Cleanup
            final FileEventStore fileEventStore = new FileEventStore(properties);
            final AddLightEvent event = new AddLightEvent(Instant.now(), new LightId(UUID.randomUUID()));
            fileEventStore.storeEvent(event);
            final List<StoredEvent> storedEvents = fileEventStore.readEvents(false).collectList().block();
            Assert.assertEquals(1, storedEvents.size());
            Assert.assertEquals(event, storedEvents.get(0));
            log.info("Stored events: " + storedEvents);
        }
        cleanup(properties);
    }

    @Test
    public void testStoreLiveEvents() throws Exception {
        final WallMenuProperties properties = initProperties();
        final FileEventStore fileEventStore = new FileEventStore(properties);
        final AddLightEvent event1 = new AddLightEvent(Instant.now(), new LightId(UUID.randomUUID()));
        fileEventStore.storeEvent(event1);
        final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(3);
        final Semaphore sem = new Semaphore(0);
        fileEventStore.readEvents(true).subscribe(queue::add, queue::add, sem::release);
        final RemoveLightEvent event2 = new RemoveLightEvent(Instant.now(), event1.getId());
        fileEventStore.storeEvent(event2);
        fileEventStore.close();
        sem.acquire();
        while (true) {
            final Object result = queue.poll(2, TimeUnit.SECONDS);
            if (result == null || result instanceof TimeoutException) {
                break;
            }
            if (result instanceof Exception) {
                throw (Exception) result;
            }
            if (!(result instanceof StoredEvent)) {
                throw new AssertionError("Wrong result type " + result);
            }
            final StoredEvent storedEvents = (StoredEvent) result;
            System.out.println(storedEvents);
        }
    }
}
