/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer.api;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Singleton;

public class MicrometerCdiExtension implements Extension {
    private final MeterRegistry registry;

    public MicrometerCdiExtension(MeterRegistry registry) {
        this.registry = registry;
    }

    public void registerMicrometerBeans(@Observes AfterBeanDiscovery abd) {
        abd.addBean()
                .scope(Singleton.class)
                .addTransitiveTypeClosure(MeterRegistry.class)
                .produceWith(i -> registry);
    }

}
