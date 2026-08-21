/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.otel.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.opentelemetry.api.trace.Span;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class TestReactiveMessagingOtelBean {

    private final List<String> received = Collections.synchronizedList(new ArrayList<>());
    private final List<String> receivedDisabledTracing = Collections.synchronizedList(new ArrayList<>());

    @Inject
    @Channel("source")
    Emitter<String> emitter;
    @Inject
    @Channel("disabled-tracing-source")
    Emitter<String> emitterDisabledTracing;

    @Incoming("disabled-tracing-sink")
    public void sinkDisabledTracing(String word) {
        System.out.println("[disabled-tracing-sink] Received  " + word);
        // Extract test metadata and set span attributes
        String actualWord = extractAndSetSpanAttributes(word);
        receivedDisabledTracing.add(actualWord);
    }

    @Incoming("sink")
    public void sink(String word) {
        System.out.println("[sink] Received " + word);
        // Extract test metadata and set span attributes
        String actualWord = extractAndSetSpanAttributes(word);
        received.add(actualWord);
    }

    private String extractAndSetSpanAttributes(String message) {
        // Message format: "testName|iteration|value" or just "value" for backward compatibility
        if (message != null && message.contains("|")) {
            String[] parts = message.split("\\|", 3);
            if (parts.length == 3) {
                String testName = parts[0];
                String iteration = parts[1];
                String value = parts[2];

                // Try to set span attributes on current span (if there's an active recording span)
                Span currentSpan = Span.current();
                if (currentSpan.isRecording()) {
                    currentSpan.setAttribute("test.name", testName);
                    try {
                        currentSpan.setAttribute("test.iteration", Integer.parseInt(iteration));
                    } catch (NumberFormatException e) {
                        // Ignore if iteration is not a valid number
                    }
                }

                return value;
            }
        }
        return message;
    }

    public void send(String word, String testName, Integer iteration) {
        // Embed test metadata in message for trace filtering
        // Format: "testName|iteration|value"
        String messageWithMetadata = (testName != null && iteration != null)
            ? testName + "|" + iteration + "|" + word
            : word;

        System.out.println("Sending " + messageWithMetadata);
        long end = System.currentTimeMillis() + 30000;
        // Workaround
        // TODO https://issues.redhat.com/browse/WFLY-19825 Remove this
        while (!emitter.hasRequests() && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        if (emitter.hasRequests()) {
            emitter.send(messageWithMetadata);
        } else {
            throw new IllegalStateException("Emitter was not ready in 30 seconds");
        }
        if (emitterDisabledTracing.hasRequests()) {
            emitterDisabledTracing.send(messageWithMetadata);
        } else {
            throw new IllegalStateException("Emitter was not ready in 30 seconds");
        }
    }

    // Backward compatibility: support old signature without test metadata
    public void send(String word) {
        send(word, null, null);
    }

    public List<String> getReceived() {
        synchronized (received) {
            // we send same messages to two channels, so we send empty list to signal they are not synced
            if (received.size() != receivedDisabledTracing.size()) {
                return Collections.emptyList();
            }
            List<String> diff = new ArrayList<>(received);
            diff.removeAll(receivedDisabledTracing);
            if (!diff.isEmpty()) {
                return Collections.emptyList();
            }
            return new ArrayList<>(received);
        }
    }
}
