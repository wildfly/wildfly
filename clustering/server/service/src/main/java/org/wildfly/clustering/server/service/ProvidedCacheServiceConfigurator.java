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
 * A {@link CapabilityServiceConfigurator} facade for collecting, configuring, and building; or removing; a set of {@link ServiceConfigurator} instances acquired from a {@link CacheServiceConfiguratorProvider}.
 * @author Paul Ferraro
 */
public class ProvidedCacheServiceConfigurator<P extends CacheServiceConfiguratorProvider> extends CompositeCapabilityServiceConfigurator {

    public ProvidedCacheServiceConfigurator(Class<P> providerClass, String containerName, String cacheName) {
        super((support, consumer) -> {
            for (P provider : ServiceLoader.load(providerClass, providerClass.getClassLoader())) {
                for (ServiceConfigurator configurator : provider.getServiceConfigurators(support, containerName, cacheName)) {
                    consumer.accept(configurator);
                }
            }
        });
    }
}
