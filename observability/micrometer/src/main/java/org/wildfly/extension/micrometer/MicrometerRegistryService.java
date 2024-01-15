/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerExtensionLogger.MICROMETER_LOGGER;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MICROMETER_REGISTRY_RUNTIME_CAPABILITY;

import java.io.IOException;
import java.util.function.Supplier;

import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.micrometer.jmx.JmxMicrometerCollector;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;

class MicrometerRegistryService implements Service {
    private WildFlyCompositeRegistry registry;

    /**
     * Installs a service that provides {@link WildFlyRegistry}, and provides a {@link Supplier} the
     * subsystem can use to obtain that registry.
     *
     * @param context         the management operation context to use to install the service. Cannot be {@code null}
     * @param wildFlyRegistry
     * @return the {@link Supplier}. Will not return {@code null}.
     */
    static void install(OperationContext context, WildFlyCompositeRegistry wildFlyRegistry) {
        CapabilityServiceBuilder<?> serviceBuilder = context.getCapabilityServiceTarget()
                .addCapability(MICROMETER_REGISTRY_RUNTIME_CAPABILITY);

        serviceBuilder.setInstance(new MicrometerRegistryService(wildFlyRegistry))
                .install();
    }

    private MicrometerRegistryService(WildFlyCompositeRegistry wildFlyRegistry) {
        this.registry = wildFlyRegistry;
    }

    @Override
    public void start(StartContext context) {
        try {
            new JmxMicrometerCollector(registry).init();
        } catch (IOException e) {
            throw MICROMETER_LOGGER.failedInitializeJMXRegistrar(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (registry != null) {
            registry.close();
            registry = null;
        }
    }
}
