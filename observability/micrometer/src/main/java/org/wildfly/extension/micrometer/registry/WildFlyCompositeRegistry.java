/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.registry;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

public class WildFlyCompositeRegistry extends CompositeMeterRegistry implements WildFlyRegistry {
//    private final List<WildFlyRegistry> registries = new ArrayList<>();

    public WildFlyCompositeRegistry() {
        super();
    }

    public void addRegistry(WildFlyRegistry registry) {
        this.add((MeterRegistry) registry);
//        this.registries.add(registry);
    }
}
