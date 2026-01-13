/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.collector;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.google.protobuf.ByteString;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import org.apache.commons.collections.KeyValue;
import org.apache.commons.collections.keyvalue.DefaultKeyValue;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.signals.trace.Span;
import org.jboss.as.test.shared.observability.signals.trace.Trace;

public class InMemoryCollector {
    private static final Logger logger = Logger.getLogger(InMemoryCollector.class.getName());
    protected final int port;
    private final List<Trace> tracesReceived = new ArrayList<>();
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(
            TimeoutUtil.adjust(
                    Integer.parseInt(
                            System.getProperty("testsuite.integration.container.timeout", "120"))));
    protected Boolean loggingEnabled; // Default: null/false
    private Server server;

    public InMemoryCollector(int port) {
        this.port = port;
        checkForLogging();
    }

    public static KeyValue convertKeyValue(io.opentelemetry.proto.common.v1.KeyValue keyValue) {
        return new DefaultKeyValue(keyValue.getKey(), keyValue.getValue().getStringValue());
    }

    public static String fromByteString(ByteString bs) {
        return HexFormat.of().formatHex(bs.toByteArray());
    }

    public void reset() {
        tracesReceived.clear();
    }

    public List<Trace> assertTraces(String serviceName, Consumer<List<Trace>> assertionConsumer) throws InterruptedException {
        return assertTraces(serviceName, DEFAULT_TIMEOUT, assertionConsumer);
    }

    /**
     * Variant of {@link OpenTelemetryCollectorContainer#assertTraces(String, Consumer)} that can be configured with a
     * timeout duration.
     */
    public List<Trace> assertTraces(String serviceName, Duration timeout, Consumer<List<Trace>> assertionConsumer) throws InterruptedException {
        Instant endTime = Instant.now().plus(timeout);
        AssertionError lastAssertionError = null;

        while (Instant.now().isBefore(endTime)) {
            try {
                assertionConsumer.accept(tracesReceived);
                debugLog("assertTraces(...) validation passed.");
                return tracesReceived;
            } catch (AssertionError assertionError) {
                debugLog("assertTraces(...) validation failed - retrying.");
                lastAssertionError = assertionError;
                Thread.sleep(1000);
            }
        }

        debugLog("assertTraces(...) validation failed. State at final check:\n" + tracesReceived);
        throw Objects.requireNonNullElseGet(lastAssertionError, AssertionError::new);
    }

    public void start() throws IOException {
        server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .addService(new LogsServiceImpl())
                .addService(new MetricsServiceImpl())
                .addService(new TraceServiceImpl())
                .build()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
        }));
        debugLog("Server started, listening on " + port);
    }

    public void shutdown() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void checkForLogging() {
        loggingEnabled =
                Boolean.parseBoolean(System.getenv().get("TC_LOGGING")) ||
                        Boolean.parseBoolean(System.getProperty("testsuite.integration.container.logging")) ||
                        Boolean.parseBoolean(System.getProperty("testsuite.integration.container.InMemoryCollector.logging"));
    }

    protected void debugLog(String message) {
        if (loggingEnabled) {
            System.err.println("[InMemoryCollector] " + message);
        }
    }

    class TraceServiceImpl extends TraceServiceGrpc.TraceServiceImplBase {
        @Override
        public void export(ExportTraceServiceRequest request,
                           StreamObserver<ExportTraceServiceResponse> responseObserver) {
            try {
                debugLog("Trace received request");
                var list = request.getResourceSpansList();
                list.forEach(rs -> {
                    rs.getScopeSpansList().forEach(ss -> {
                        ss.getSpansList().forEach(s -> {
                            String traceId = fromByteString(s.getTraceId());

                            Span span = Span.builder()
                                    .traceId(traceId)
                                    .spanId(fromByteString(s.getSpanId()))
                                    .name(s.getName())
                                    .kind(s.getKindValue())
                                    .traceState(s.getTraceState())
                                    .parentSpanId(fromByteString(s.getParentSpanId()))
                                    .flags(s.getFlags())
                                    .startTimeUnixNano(s.getStartTimeUnixNano())
                                    .endTimeUnixNano(s.getEndTimeUnixNano())
                                    .attributes(s.getAttributesList())
                                    .droppedAttributesCount(s.getDroppedAttributesCount())
                                    .events(s.getEventsList())
                                    .droppedEventsCount(s.getDroppedEventsCount())
                                    .links(s.getLinksList())
                                    .droppedLinksCount(s.getDroppedLinksCount())
                                    .status(s.getStatus()).build();

                            Trace trace = tracesReceived.stream().filter(t ->
                                    t.traceId().equals(traceId)).findFirst().orElseGet(() -> {
                                var newTrace = new Trace(traceId);
                                tracesReceived.add(newTrace);
                                return newTrace;
                            });
                            trace.addSpan(span);
                        });
                    });
                });

                ExportTraceServiceResponse response = ExportTraceServiceResponse.newBuilder()
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                responseObserver.onError(e);
            }
        }
    }

    class LogsServiceImpl extends LogsServiceGrpc.LogsServiceImplBase {
        @Override
        public void export(ExportLogsServiceRequest request,
                           StreamObserver<ExportLogsServiceResponse> responseObserver) {
            try {
                debugLog("Logs received request");
                var list = request.getResourceLogsList();
                list.forEach(rl -> {
                    rl.getScopeLogsList().forEach(sl -> {
                        sl.getLogRecordsList().forEach(lr -> {
                            debugLog("Log message: " + lr.getBody().getStringValue());
                        });
                    });
                });
                ExportLogsServiceResponse response = ExportLogsServiceResponse.newBuilder()
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                responseObserver.onError(e);
            }
        }
    }

    class MetricsServiceImpl extends MetricsServiceGrpc.MetricsServiceImplBase {
        @Override
        public void export(ExportMetricsServiceRequest request,
                           StreamObserver<ExportMetricsServiceResponse> responseObserver) {
            try {
                debugLog("Metrics received request");
                var list = request.getResourceMetricsList();
                list.forEach(rm -> {
                    rm.getScopeMetricsList().forEach(sm -> {
                        sm.getMetricsList().forEach(m -> {
                            debugLog("Metric name: " + m.getName());
                        });
                    });
                });
                ExportMetricsServiceResponse response = ExportMetricsServiceResponse.newBuilder()
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                responseObserver.onError(e);
            }
        }
    }
}
