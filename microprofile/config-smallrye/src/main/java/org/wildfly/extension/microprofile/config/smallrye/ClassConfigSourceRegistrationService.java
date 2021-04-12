/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

    static void install(OperationContext context, String name, ConfigSource configSource, Registry registry) {
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
