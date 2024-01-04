/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.microprofile.config.smallrye._private.MicroProfileConfigLogger;

/**
 * Service to register a ConfigSource reading its configuration from a directory (where files are property keys and
 * their content is the property values).
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
class ClassConfigSourceRegistrationService implements Service {

    private final String name;
    private final ConfigSource configSource;
    private final Registry<ConfigSource> sources;

    ClassConfigSourceRegistrationService(String name, ConfigSource configSource, Registry<ConfigSource> sources) {
        this.name = name;
        this.configSource = configSource;
        this.sources = sources;
    }

    static void install(OperationContext context, String name, ConfigSource configSource, Registry<ConfigSource> registry) {
        ServiceBuilder<?> builder = context.getServiceTarget()
                .addService(ServiceNames.CONFIG_SOURCE.append(name));

        builder.setInstance(new ClassConfigSourceRegistrationService(name, configSource, registry))
                .install();
    }

    @Override
    public void start(StartContext startContext) {
        MicroProfileConfigLogger.ROOT_LOGGER.loadConfigSourceFromClass(configSource.getClass());
        this.sources.register(this.name, configSource);
    }

    @Override
    public void stop(StopContext context) {
        this.sources.unregister(this.name);
    }
}
