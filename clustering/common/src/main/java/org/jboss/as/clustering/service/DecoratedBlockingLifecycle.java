/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.service;

import org.wildfly.service.BlockingLifecycle;

/**
 * A blocking lifecycle decorator.
 */
public class DecoratedBlockingLifecycle implements BlockingLifecycle {
    private final BlockingLifecycle lifecycle;

    protected DecoratedBlockingLifecycle(BlockingLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public boolean isStarted() {
        return this.lifecycle.isStarted();
    }

    @Override
    public boolean isStopped() {
        return this.lifecycle.isStopped();
    }

    @Override
    public boolean isClosed() {
        return this.lifecycle.isClosed();
    }

    @Override
    public void start() {
        this.lifecycle.start();
    }

    @Override
    public void stop() {
        this.lifecycle.stop();
    }

    @Override
    public void close() {
        this.lifecycle.close();
    }
}
