/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
class ConfigSourceService implements Service<ConfigSource> {

    private final String name;
    private final ConfigSource configSource;

    ConfigSourceService(String name, ConfigSource configSource) {
        this.name = name;
        this.configSource = configSource;
    }

    static void install(OperationContext context, String name, ConfigSource configSource) {
        context.getServiceTarget()
                .addService(ServiceNames.CONFIG_SOURCE.append(name))
                .setInstance(new ConfigSourceService(name, configSource))
                .install();
    }

    @Override
    public void start(StartContext startContext) {
    }

    @Override
    public void stop(StopContext stopContext) {
    }

    @Override
    public ConfigSource getValue() throws IllegalStateException, IllegalArgumentException {
        return configSource;
    }
}
