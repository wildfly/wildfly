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
 * A {@link CapabilityServiceConfigurator} facade for collecting, configuring, and building; or removing; a set of {@link ServiceConfigurator} instances acquired from a {@link GroupServiceConfiguratorProvider}.
 * @author Paul Ferraro
 */
public class ProvidedGroupServiceConfigurator<P extends GroupServiceConfiguratorProvider> extends CompositeCapabilityServiceConfigurator {

    public ProvidedGroupServiceConfigurator(Class<P> providerClass, String group) {
        super((support, consumer) -> {
            for (P provider : ServiceLoader.load(providerClass, providerClass.getClassLoader())) {
                for (ServiceConfigurator configurator : provider.getServiceConfigurators(support, group)) {
                    consumer.accept(configurator);
                }
            }
        });
    }
}
