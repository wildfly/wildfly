/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.amqp;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class ConsumingBean {

    volatile long last = -1;

    @Incoming("in")
    public void consume(long content) {
        System.out.println("---> Received " + content);
        last = content;
    }

    public long get() {
        return last;
    }

}
