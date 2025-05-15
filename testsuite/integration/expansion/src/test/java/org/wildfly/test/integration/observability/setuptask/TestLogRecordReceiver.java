package org.wildfly.test.integration.observability.setuptask;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.wildfly.test.integration.observability.opentelemetry.exporter.TestLogRecordData;

public class TestLogRecordReceiver {
    private final HttpServer server;
    private List<LogRecordData> logs = Collections.synchronizedList(new ArrayList<>());
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(
        TimeoutUtil.adjust(Integer.parseInt(System.getProperty("testsuite.integration.container.timeout", "30"))));

    public TestLogRecordReceiver(int port) {
        try {
            // Create an HttpServer instance
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // Create a context for a specific path and set the handler
            server.createContext("/", new LogRecordHandler());

            // Start the server
            server.setExecutor(null); // Use the default executor
            server.start();

            System.out.println("Server is running on port " + port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public List<LogRecordData> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    public void reset() {
        logs.clear();
    }

    public List<LogRecordData> assertLogs(Consumer<List<LogRecordData>> assertionConsumer) throws InterruptedException {
        return assertLogs(assertionConsumer, DEFAULT_TIMEOUT);
    }

    /**
     * Variant of {@link OpenTelemetryCollectorContainer#assertTraces(String, Consumer)} that can be configured with a
     * timeout duration.
     */
    public List<LogRecordData> assertLogs(Consumer<List<LogRecordData>> assertionConsumer, Duration timeout) throws InterruptedException {
        Instant endTime = Instant.now().plus(timeout);
        AssertionError lastAssertionError = null;

        while (Instant.now().isBefore(endTime)) {
            try {
                assertionConsumer.accept(logs);
                System.out.println(logs);
                return logs;
            } catch (AssertionError assertionError) {
                lastAssertionError = assertionError;
                Thread.sleep(1000);
            }
        }

        throw Objects.requireNonNullElseGet(lastAssertionError, AssertionError::new);
    }

    private class LogRecordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                logs.addAll(deserialize(new String(exchange.getRequestBody().readAllBytes())));

                sendResponse(exchange,200, "OK");
            } catch (Exception e) {
                sendResponse(exchange,500, e.getMessage());
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.sendResponseHeaders(statusCode, response.length());

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private List<LogRecordData> deserialize(String json) {
            var jsonReader = Json.createReader(new StringReader(json));
            List<LogRecordData> list = new ArrayList<>();

            var array = jsonReader.readArray();

            array.forEach(record -> {
                JsonObject object = record.asJsonObject();
                list.add(TestLogRecordData.fromJson(object));
            });

            return list;
        }
    }
}
