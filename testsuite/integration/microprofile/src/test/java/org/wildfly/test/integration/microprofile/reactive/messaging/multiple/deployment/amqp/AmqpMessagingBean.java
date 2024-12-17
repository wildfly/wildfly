/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.multiple.deployment.amqp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class AmqpMessagingBean {
    private final List<String> received = Collections.synchronizedList(new ArrayList<>());

    @Inject
    @Channel("source")
    Emitter<String> emitter;

    @Incoming("sink")
    public void sink(String word) {
        System.out.println("Received " + word);
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
    }

    public List<String> getReceived() {
        synchronized (received) {
            return new ArrayList<>(received);
        }
    }
}
