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
        receivedDisabledTracing.add(word);
    }

    @Incoming("sink")
    public void sink(String word) {
        System.out.println("[sink] Received " + word);
        received.add(word);
    }

    public void send(String word) {
        System.out.println("Sending " + word);
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
            emitter.send(word);
        } else {
            throw new IllegalStateException("Emitter was not ready in 30 seconds");
        }
        if (emitterDisabledTracing.hasRequests()) {
            emitterDisabledTracing.send(word);
        } else {
            throw new IllegalStateException("Emitter was not ready in 30 seconds");
        }
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
