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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;

import io.micrometer.core.instrument.MeterRegistry;
import org.wildfly.security.manager.WildFlySecurityManager;

public class MicrometerCdiExtension implements Extension {
    private static final Map<ClassLoader, MeterRegistry> REGISTRY_INSTANCES = Collections.synchronizedMap(new WeakHashMap<>());

    public static MeterRegistry registerApplicationRegistry(ClassLoader classLoader, MeterRegistry registry) {
        REGISTRY_INSTANCES.put(classLoader, registry);

        return registry;
    }

    public void registerMicrometerBeans(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        abd.addBean()
                .addTransitiveTypeClosure(MeterRegistry.class)
                .produceWith(i -> REGISTRY_INSTANCES.get(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged()));
    }

    public void beforeShutdown(@Observes final BeforeShutdown bs) {
        REGISTRY_INSTANCES.remove(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }
}
