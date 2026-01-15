/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.collector;

import static org.jboss.as.test.shared.observability.collector.CollectorUtil.debugLog;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import jakarta.servlet.ServletException;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.observability.collector.grpc.LogsServiceImpl;
import org.jboss.as.test.shared.observability.collector.grpc.MetricsServiceImpl;
import org.jboss.as.test.shared.observability.collector.grpc.TraceServiceImpl;
import org.jboss.as.test.shared.observability.collector.http.UndertowServer;
import org.jboss.as.test.shared.observability.signals.SimpleMetric;
import org.jboss.as.test.shared.observability.signals.logs.LogEntry;
import org.jboss.as.test.shared.observability.signals.trace.Span;

public class InMemoryCollector {
    private static final Logger logger = Logger.getLogger(InMemoryCollector.class.getName());
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(
            TimeoutUtil.adjust(Integer.parseInt(System.getProperty("testsuite.integration.container.timeout", "120"))));

    private final List<Span> spansReceived = Collections.synchronizedList(new ArrayList<>());
    private final List<LogEntry> logsReceived = Collections.synchronizedList(new ArrayList<>());
    private final List<SimpleMetric> metricsReceived = Collections.synchronizedList(new ArrayList<>());

    protected int grpcPort = 4317;
    protected int httpPort = 4318;

    private Server grpcServer;
    private UndertowServer undertowServer;

    public InMemoryCollector() {
    }

    public InMemoryCollector(int gprcPort, int httpPort) {
        this.grpcPort = gprcPort;
        this.httpPort = httpPort;
    }

    public synchronized void reset() {
        logsReceived.clear();
        spansReceived.clear();
        metricsReceived.clear();
    }

    public List<LogEntry> assertLogs(Consumer<List<LogEntry>> assertionConsumer) throws InterruptedException {
        return assertLoop(DEFAULT_TIMEOUT, logsReceived, assertionConsumer);
    }

    public List<SimpleMetric> assertMetrics(Consumer<List<SimpleMetric>> assertionConsumer) throws InterruptedException {
        return assertLoop(DEFAULT_TIMEOUT, metricsReceived, assertionConsumer);
    }

    public List<Span> assertSpans(Consumer<List<Span>> assertionConsumer) throws InterruptedException {
        return assertLoop(DEFAULT_TIMEOUT, spansReceived, assertionConsumer);
    }

    public void start() throws IOException {
        startGrpcServer();
        startHttpServer();
        debugLog("Grpc server listening on " + grpcPort);
        debugLog("Https server listening on " + httpPort);
    }

    public void shutdown() throws InterruptedException {
        if (grpcServer != null) {
            grpcServer.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
        if (undertowServer != null) {
            undertowServer.shutdown();
        }
    }

    public List<LogEntry> fetchLogs() {
        return Collections.unmodifiableList(logsReceived);
    }

    public List<SimpleMetric> fetchMetrics() {
        return Collections.unmodifiableList(metricsReceived);
    }

    private void startGrpcServer() throws IOException {
        grpcServer = Grpc.newServerBuilderForPort(grpcPort, InsecureServerCredentials.create())
                .addService(new LogsServiceImpl(logsReceived::add))
                .addService(new MetricsServiceImpl(metricsReceived::add))
                .addService(new TraceServiceImpl(this::consumeSpan))
                .build()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
        }));
    }

    private void startHttpServer() throws IOException {
        undertowServer = new UndertowServer(this::consumeSpan, metricsReceived::add);
        try {
            undertowServer.start();
        } catch (ServletException e) {
            throw new IOException(e);
        }
    }

    private void consumeSpan(Span s) {
        spansReceived.add(s);
    }

    // Meters are constantly re-published, so a value for meter A will be received multiple times. We are only interested
    // in the latest value, though, so we remove any existing meter value, then add the new.
    private synchronized void consumeSimpleMeter(SimpleMetric sm) {
        metricsReceived.remove(sm);
        metricsReceived.add(sm);
    }

    private <T> List<T> assertLoop(Duration timeout,
                                   List<T> received,
                                   Consumer<List<T>> consumer) throws InterruptedException {
        Instant endTime = Instant.now().plus(timeout);
        AssertionError lastAssertionError = null;

        while (Instant.now().isBefore(endTime)) {
            try {
                consumer.accept(new ArrayList<>(received));
                debugLog("assertLoop(...) validation passed.");
                return received;
            } catch (AssertionError assertionError) {
                debugLog("assertLoop(...) validation failed - retrying.");
                lastAssertionError = assertionError;
                //noinspection BusyWait
                Thread.sleep(1000);
            }
        }

        debugLog("assertLoop(...) validation failed. State at final check:\n" + received);
        throw Objects.requireNonNullElseGet(lastAssertionError, AssertionError::new);
    }
}
