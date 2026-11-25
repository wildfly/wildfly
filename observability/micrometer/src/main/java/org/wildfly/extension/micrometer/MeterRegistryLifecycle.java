/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.wildfly.service.BlockingLifecycle;

/**
 * Instruments the lifecycle of a meter registry.
 * @author Paul Ferraro
 */
public class MeterRegistryLifecycle implements BlockingLifecycle {
    private final MeterRegistry registry;
    private final CompositeMeterRegistry compositeRegistry;

    public MeterRegistryLifecycle(MeterRegistry registry, CompositeMeterRegistry compositeRegistry) {
        this.compositeRegistry = compositeRegistry;
        this.registry = registry;
    }

    @Override
    public boolean isClosed() {
        return this.registry.isClosed();
    }

    @Override
    public boolean isStarted() {
        return this.compositeRegistry.getRegistries().contains(this.registry);
    }

    @Override
    public void start() {
        this.compositeRegistry.add(this.registry);
    }

    @Override
    public void stop() {
        this.compositeRegistry.remove(this.registry);
    }

    @Override
    public void close() {
        this.registry.close();
    }
}
