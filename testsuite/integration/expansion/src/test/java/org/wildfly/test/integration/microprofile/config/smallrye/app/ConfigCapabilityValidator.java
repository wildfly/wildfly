/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.app;

import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * ServiceActivator that validates the microprofile-config capability service is properly installed.
 * Stores the ConfigProviderResolver from the capability in a static field so REST endpoints can access it.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ConfigCapabilityValidator implements ServiceActivator {

    // Static holder for the resolver from the capability service
    private static volatile ConfigProviderResolver capabilityResolver;
    private static volatile Config capabilityConfig;

    @Override
    public void activate(ServiceActivatorContext context) {
        ServiceBuilder<?> builder = context.getServiceTarget()
                .addService();

        // Require the microprofile-config capability service
        // Use parse() to treat the whole string as a single service name
        ServiceName capabilityServiceName = ServiceName.parse("org.wildfly.microprofile.config");

        Supplier<ConfigProviderResolver> resolverSupplier = builder.requires(capabilityServiceName);

        builder.setInstance(new Service() {
            @Override
            public void start(StartContext context) {
                // Get the ConfigProviderResolver from the capability service
                ConfigProviderResolver resolver = resolverSupplier.get();

                // Store it in static field for REST endpoint to access
                capabilityResolver = resolver;

                // Also get the Config instance
                capabilityConfig = resolver.getConfig();
            }

            @Override
            public void stop(StopContext context) {
                capabilityResolver = null;
                capabilityConfig = null;
            }
        });

        builder.install();
    }

    /**
     * Get the ConfigProviderResolver that was injected from the capability service.
     * @return the resolver, or null if service hasn't started yet
     */
    public static ConfigProviderResolver getCapabilityResolver() {
        return capabilityResolver;
    }

    /**
     * Get the Config instance from the capability resolver.
     * @return the config, or null if service hasn't started yet
     */
    public static Config getCapabilityConfig() {
        return capabilityConfig;
    }
}
