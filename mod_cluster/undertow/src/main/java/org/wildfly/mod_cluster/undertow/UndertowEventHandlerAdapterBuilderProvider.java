/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.mod_cluster.undertow;

import java.time.Duration;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.kohsuke.MetaInfServices;
import org.wildfly.extension.mod_cluster.ContainerEventHandlerAdapterServiceConfiguratorProvider;

/**
 * {@link ContainerEventHandlerAdapterServiceConfiguratorProvider} service provider for Undertow.
 *
 * @author Paul Ferraro
 */
@MetaInfServices(ContainerEventHandlerAdapterServiceConfiguratorProvider.class)
public class UndertowEventHandlerAdapterBuilderProvider implements ContainerEventHandlerAdapterServiceConfiguratorProvider {

    @Override
    public CapabilityServiceConfigurator getServiceConfigurator(String name, String listenerName, Duration statusInterval) {
        return new UndertowEventHandlerAdapterServiceConfigurator(name, listenerName, statusInterval);
    }
}
