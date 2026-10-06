package com.quantum.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Plays the timed sequence on the ESP32. A new Run cancels the previous one. A missing device never breaks the API. */
@Component
public class DeviceClient {

    private static final System.Logger LOG = System.getLogger("device");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "device-sequence");
        t.setDaemon(true);
        return t;
    });
    private final List<ScheduledFuture<?>> pending = new ArrayList<>();
    private final ObjectMapper mapper;
    private final String url;

    public DeviceClient(ObjectMapper mapper, @Value("${qcore.esp32.url:}") String url) {
        this.mapper = mapper;
        this.url = url;
    }

    public boolean enabled() { return url != null && !url.isBlank(); }

    public synchronized void play(List<DeviceEvent> events) {
        if (!enabled()) return;
        pending.forEach(f -> f.cancel(false));
        pending.clear();
        for (DeviceEvent e : events) {
            pending.add(executor.schedule(() -> post(e), e.atMs(), TimeUnit.MILLISECONDS));
        }
    }

    private void post(DeviceEvent e) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url + "/state"))
                    .timeout(Duration.ofSeconds(2))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(e)))
                    .build();
            http.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ex) {
            LOG.log(System.Logger.Level.WARNING, "ESP32 not reachable at {0}: {1}", url, ex.getMessage());
        }
    }
}
