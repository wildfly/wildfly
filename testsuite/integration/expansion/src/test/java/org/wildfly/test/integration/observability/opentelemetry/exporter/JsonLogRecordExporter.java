/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry.exporter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.Collection;

import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

public class JsonLogRecordExporter implements LogRecordExporter {
    private final String remoteUri;
    private final HttpClient client = HttpClient.newHttpClient();

    public JsonLogRecordExporter() {
        if (System.getSecurityManager() == null) {
            remoteUri = System.getProperty("wildfly.opentelemetry.logs.json.exporter",
                "http://localhost:1223");
        } else {
            remoteUri = AccessController.doPrivileged(
                (PrivilegedAction<String>) () -> System.getProperty("wildfly.opentelemetry.logs.json.exporter",
                    "http://localhost:1223")
            );
        }
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        try {
            String json = encodeToJson(logs);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(remoteUri))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());
            return (response.statusCode() == 200) ?
                CompletableResultCode.ofSuccess() :
                CompletableResultCode.ofFailure();
        } catch (IOException | InterruptedException e) {
            return CompletableResultCode.ofFailure();
        }
    }

    private String encodeToJson(Collection<LogRecordData> logs) {
        StringBuilder builder = new StringBuilder("[");
        String sep = "";

        for (LogRecordData log : logs) {
            SpanContext spanContext = log.getSpanContext();
            JsonObjectBuilder spanContextBuilder = Json.createObjectBuilder();
            spanContextBuilder.add("traceId", spanContext.getTraceId());
            spanContextBuilder.add("spanId", spanContext.getSpanId());
            spanContextBuilder.add("valid", spanContext.isValid());
            spanContextBuilder.add("remote", spanContext.isRemote());

            JsonObjectBuilder logBuilder = Json.createObjectBuilder();
            logBuilder.add("timestampEpochNanos", log.getTimestampEpochNanos());
            logBuilder.add("observedTimestampEpochNanos", log.getObservedTimestampEpochNanos());
            logBuilder.add("spanContext", spanContextBuilder.build().asJsonObject());
            logBuilder.add("severity", log.getSeverity().name().toUpperCase());
            String severityText = log.getSeverityText();
            if (severityText != null) {
                logBuilder.add("severityText", severityText.toUpperCase());
            }
            logBuilder.add("totalAttributeCount", log.getTotalAttributeCount());
            Value<?> bodyValue = log.getBodyValue();
            if (bodyValue != null) {
                logBuilder.add("bodyValue", bodyValue.asString());
            }

            builder.append(sep).append(logBuilder.build().toString());
            sep = ",";
        }

        return builder.append("]").toString();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
