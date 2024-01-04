/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
