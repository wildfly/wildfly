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
import org.jboss.as.test.shared.observability.signals.trace.SimpleSpan;

public class InMemoryCollector {
    private static InMemoryCollector INSTANCE;

    private static final Logger logger = Logger.getLogger(InMemoryCollector.class.getName());
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(
            TimeoutUtil.adjust(Integer.parseInt(System.getProperty("testsuite.integration.container.timeout", "120"))));

    private final List<SimpleSpan> spansReceived = Collections.synchronizedList(new ArrayList<>());
    private final List<LogEntry> logsReceived = Collections.synchronizedList(new ArrayList<>());
    private final List<SimpleMetric> metricsReceived = Collections.synchronizedList(new ArrayList<>());

    protected final int grpcPort = 4317;
    protected final int httpPort = 4318;

    private Server grpcServer;
    private UndertowServer undertowServer;

    private InMemoryCollector() {
        start();
    }

    public static InMemoryCollector getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new InMemoryCollector();
        }
        return INSTANCE;
    }

    public void shutdown() throws InterruptedException {
        if (undertowServer != null) {
            undertowServer.shutdown();
        }
        if (grpcServer != null) {
            grpcServer.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }

        INSTANCE = null;
    }

    public synchronized void reset() {
        logsReceived.clear();
        spansReceived.clear();
        metricsReceived.clear();
    }

    public List<LogEntry> assertLogs(Consumer<List<LogEntry>> assertionConsumer) throws InterruptedException {
        return assertLoop(DEFAULT_TIMEOUT, logsReceived, assertionConsumer);
    }

    public List<LogEntry> assertLogs(Duration timeout, Consumer<List<LogEntry>> assertionConsumer) throws InterruptedException {
        return assertLoop(timeout, logsReceived, assertionConsumer);
    }

    public List<SimpleMetric> assertMetrics(Consumer<List<SimpleMetric>> assertionConsumer) throws InterruptedException {
        return assertLoop(DEFAULT_TIMEOUT, metricsReceived, assertionConsumer);
    }

    public List<SimpleMetric> assertMetrics(Duration timeout, Consumer<List<SimpleMetric>> assertionConsumer) throws InterruptedException {
        return assertLoop(timeout, metricsReceived, assertionConsumer);

    }

    public List<SimpleSpan> assertSpans(Consumer<List<SimpleSpan>> assertionConsumer) throws InterruptedException {
        return assertLoop(DEFAULT_TIMEOUT, spansReceived, assertionConsumer);
    }

    public List<SimpleSpan> assertSpans(Duration timeout, Consumer<List<SimpleSpan>> assertionConsumer) throws InterruptedException {
        return assertLoop(timeout, spansReceived, assertionConsumer);

    }

    public List<LogEntry> fetchLogs() {
        return new ArrayList<>(logsReceived);
    }

    public List<SimpleMetric> fetchMetrics() {
        return new ArrayList<>(metricsReceived);
    }

    public List<SimpleSpan> fetchSpans() {
        return new ArrayList<>(spansReceived);
    }

    private void start() {
        startGrpcServer();
        startHttpServer();
        debugLog("Grpc server listening on " + grpcPort);
        debugLog("Https server listening on " + httpPort);
    }

    private synchronized void startGrpcServer() {
        if (grpcServer == null) {
            try {
                grpcServer = Grpc.newServerBuilderForPort(grpcPort, InsecureServerCredentials.create())
                        .addService(new LogsServiceImpl(logsReceived::add))
                        .addService(new MetricsServiceImpl(this::consumeMeter))
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
            } catch (IOException e) {
                throw new RuntimeException("Unable to start the grpc server", e);
            }
        }
    }

    private synchronized void startHttpServer() {
        if (undertowServer == null) {
            try {
                undertowServer = new UndertowServer(this::consumeSpan, this::consumeMeter);
                undertowServer.start();
            } catch (ServletException e) {
                throw new RuntimeException("Unable to start the http server", e);

            }
        }
    }

    private void consumeSpan(SimpleSpan s) {
        spansReceived.add(s);
    }

    // Meters are constantly re-published, so a value for meter A will be received multiple times. We are only interested
    // in the latest value, though, so we remove any existing meter value, then add the new.
    private synchronized void consumeMeter(SimpleMetric sm) {
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
