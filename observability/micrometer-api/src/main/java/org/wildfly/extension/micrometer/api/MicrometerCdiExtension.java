/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.micrometer.api;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Singleton;

public class MicrometerCdiExtension implements Extension {
    private final MeterRegistry registry;

    public MicrometerCdiExtension(MeterRegistry registry) {
        this.registry = registry;
    }

    public void registerMicrometerBeans(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        abd.addBean()
                .scope(Singleton.class)
                .addTransitiveTypeClosure(MeterRegistry.class)
                .produceWith(i -> registry);
    }

}
