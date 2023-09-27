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
 * A {@link CapabilityServiceConfigurator} facade for collecting, configuring, and building; or removing; a set of {@link ServiceConfigurator} instances acquired from a {@link IdentityGroupServiceConfiguratorProvider}.
 * @author Paul Ferraro
 */
public class ProvidedIdentityGroupServiceConfigurator extends CompositeCapabilityServiceConfigurator {

    public ProvidedIdentityGroupServiceConfigurator(String group, String targetGroup) {
        super((support, consumer) -> {
            for (IdentityGroupServiceConfiguratorProvider provider : ServiceLoader.load(IdentityGroupServiceConfiguratorProvider.class, IdentityGroupServiceConfiguratorProvider.class.getClassLoader())) {
                for (ServiceConfigurator configurator : provider.getServiceConfigurators(support, group, targetGroup)) {
                    consumer.accept(configurator);
                }
            }
        });
    }
}
