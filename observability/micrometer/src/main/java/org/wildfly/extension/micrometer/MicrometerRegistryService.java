/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerExtensionLogger.MICROMETER_LOGGER;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MICROMETER_REGISTRY_RUNTIME_CAPABILITY;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.micrometer.jmx.JmxMicrometerCollector;
import org.wildfly.extension.micrometer.registry.NoOpRegistry;
import org.wildfly.extension.micrometer.registry.WildFlyOtlpRegistry;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;

class MicrometerRegistryService implements Service {
    private final Consumer<WildFlyRegistry> registriesConsumer;
    private final WildFlyMicrometerConfig config;
    private WildFlyRegistry registry;

    /**
     * Installs a service that provides {@link WildFlyRegistry}, and provides a {@link Supplier} the
     * subsystem can use to obtain that registry.
     *
     * @param context  the management operation context to use to install the service. Cannot be {@code null}
     * @param config the configuration object for the registry
     * @return the {@link Supplier}. Will not return {@code null}.
     */
    static Supplier<WildFlyRegistry> install(OperationContext context, WildFlyMicrometerConfig config) {
        CapabilityServiceBuilder<?> serviceBuilder = context.getCapabilityServiceTarget()
                .addCapability(MICROMETER_REGISTRY_RUNTIME_CAPABILITY);

        RegistrySupplier registrySupplier =
                new RegistrySupplier(serviceBuilder.provides(MICROMETER_REGISTRY_RUNTIME_CAPABILITY.getCapabilityServiceName()));
        serviceBuilder.setInstance(new MicrometerRegistryService(registrySupplier, config))
                .install();

        return registrySupplier;
    }

    private MicrometerRegistryService(Consumer<WildFlyRegistry> registriesConsumer, WildFlyMicrometerConfig config) {
        this.registriesConsumer = registriesConsumer;
        this.config = config;
    }

    @Override
    public void start(StartContext context) {
        if (config.url() != null) {
            registry = new WildFlyOtlpRegistry(config);
        } else {
            MICROMETER_LOGGER.noOpRegistryChosen();
            registry = new NoOpRegistry();
        }

        try {
            // register metrics from JMX MBeans for base metrics
            new JmxMicrometerCollector(registry).init();
        } catch (IOException e) {
            throw MICROMETER_LOGGER.failedInitializeJMXRegistrar(e);
        }

        registriesConsumer.accept(registry);
    }

    @Override
    public void stop(StopContext context) {
        if (registry != null) {
            registry.close();
            registry = null;
        }

        registriesConsumer.accept(null);
    }

    /* Caches the WildFlyRegistry created in Service.start for use by the subsystem. */
    private static final class RegistrySupplier implements Consumer<WildFlyRegistry>, Supplier<WildFlyRegistry> {
        private final Consumer<WildFlyRegistry> wrapped;
        private volatile WildFlyRegistry registry;

        private RegistrySupplier(Consumer<WildFlyRegistry> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void accept(WildFlyRegistry registry) {
            this.registry = registry;
            // Pass the registry on to MSC's consumer
            wrapped.accept(registry);
        }

        @Override
        public WildFlyRegistry get() {
            return registry;
        }
    }
}
