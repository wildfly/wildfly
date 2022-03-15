/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.service;

import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.CompositeCapabilityServiceConfigurator;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * A {@link CapabilityServiceConfigurator} facade for collecting, configuring, and building; or removing; a set of {@link ServiceConfigurator} instances acquired from a {@link IdentityCacheServiceConfiguratorProvider}.
 * @author Paul Ferraro
 */
public class ProvidedIdentityCacheServiceConfigurator extends CompositeCapabilityServiceConfigurator {

    public ProvidedIdentityCacheServiceConfigurator(String containerName, String cacheName, String targetCacheName) {
        super((support, consumer) -> {
            for (IdentityCacheServiceConfiguratorProvider provider : ServiceLoader.load(IdentityCacheServiceConfiguratorProvider.class, IdentityCacheServiceConfiguratorProvider.class.getClassLoader())) {
                for (ServiceConfigurator configurator : provider.getServiceConfigurators(support, containerName, cacheName, targetCacheName)) {
                    consumer.accept(configurator);
                }
            }
        });
    }
}
