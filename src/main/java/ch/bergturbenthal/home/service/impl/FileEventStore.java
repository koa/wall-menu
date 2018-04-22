package ch.bergturbenthal.home.service.impl;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import ch.bergturbenthal.home.model.event.StoredEvent;
import ch.bergturbenthal.home.model.settings.WallMenuProperties;
import ch.bergturbenthal.home.service.StoreService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

@Slf4j
@Service
public class FileEventStore implements StoreService, Closeable {
    private final ObjectReader                           objectReader;
    private final ObjectWriter                           objectWriter;
    private final PrintWriter                            fileWriter;
    private final File                                   eventsFile;
    private final Flux<StoredEvent>                      liveEventFlux;
    private final AtomicReference<FluxSink<StoredEvent>> liveEventSink;

    public FileEventStore(final WallMenuProperties properties) throws IOException {
        final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        objectReader = objectMapper.readerFor(StoredEvent.class);
        objectWriter = objectMapper.writerFor(StoredEvent.class);

        final File storedir = properties.getStoredir();
        if (!storedir.exists()) {
            storedir.mkdirs();
        }
        eventsFile = new File(storedir, "events");
        if (eventsFile.exists()) {
            final File tempFile = new File(storedir, "events-" + Instant.now().toEpochMilli());
            eventsFile.renameTo(tempFile);
            boolean hadError = false;
            try (PrintWriter fileWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(eventsFile), StandardCharsets.UTF_8))) {
                try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(tempFile), StandardCharsets.UTF_8))) {
                    while (true) {
                        final String line = fileReader.readLine();
                        if (line == null) {
                            break;
                        }
                        StoredEvent data = null;
                        try {
                            data = objectReader.readValue(line);

                        } catch (final IOException ex) {
                            log.info("Error decoding object " + line);
                            hadError = true;
                        }
                        if (data != null) {
                            fileWriter.println(objectWriter.writeValueAsString(data));
                        }
                    }
                }
            }
            if (!hadError) {
                tempFile.delete();
            }

        }
        fileWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(eventsFile, true), StandardCharsets.UTF_8), true);
        liveEventSink = new AtomicReference<FluxSink<StoredEvent>>(null);
        liveEventFlux = Flux.create((final FluxSink<StoredEvent> sink) -> {
            liveEventSink.set(sink);
        }).cache(1);
        liveEventFlux.subscribe();
    }

    @Override
    public void close() throws IOException {
        final FluxSink<StoredEvent> sink = liveEventSink.get();
        if (sink != null) {
            sink.complete();
        }
    }

    @Override
    public Flux<StoredEvent> readEvents(final boolean live) {
        if (live) {
            return Flux.concat(readStoredEvents(), liveEventFlux).distinctUntilChanged();
        } else {
            return readStoredEvents();
        }
    }

    private Flux<StoredEvent> readStoredEvents() {
        return Flux.create(sink -> {
            try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(eventsFile), StandardCharsets.UTF_8))) {
                while (true) {
                    final String line = fileReader.readLine();
                    if (line == null) {
                        break;
                    }
                    final StoredEvent data = objectReader.readValue(line);
                    sink.next(data);

                }
                sink.complete();
            } catch (final IOException ex) {
                sink.error(ex);
            }
        });
    }

    @Override
    public void storeEvent(final StoredEvent event) {
        synchronized (this) {
            try {
                fileWriter.println(objectWriter.writeValueAsString(event));
            } catch (final JsonProcessingException e) {
                throw new RuntimeException("Cannot write event " + event);
            }
        }
        final FluxSink<StoredEvent> sink = liveEventSink.get();
        if (sink != null) {
            sink.next(event);
        }
    }

}
