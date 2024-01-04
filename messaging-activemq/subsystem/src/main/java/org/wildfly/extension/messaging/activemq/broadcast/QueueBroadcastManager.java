/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.broadcast;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * @author Paul Ferraro
 */
public class QueueBroadcastManager implements BroadcastManager {
    private final BlockingQueue<byte[]> broadcasts = new LinkedBlockingDeque<>();
    private final String name;

    public QueueBroadcastManager(String name) {
        this.name = name;
    }

    @Override
    public void receive(byte[] broadcast) {
        if (MessagingLogger.ROOT_LOGGER.isDebugEnabled()) {
            MessagingLogger.ROOT_LOGGER.debugf("Received broadcast from group %s: %s", this.name, Arrays.toString(broadcast));
        }
        this.broadcasts.add(broadcast);
    }

    @Override
    public byte[] getBroadcast() throws InterruptedException {
        return this.broadcasts.take();
    }

    @Override
    public byte[] getBroadcast(long timeout, TimeUnit unit) throws InterruptedException {
        return this.broadcasts.poll(timeout, unit);
    }

    @Override
    public void clear() {
        this.broadcasts.clear();
    }
}
