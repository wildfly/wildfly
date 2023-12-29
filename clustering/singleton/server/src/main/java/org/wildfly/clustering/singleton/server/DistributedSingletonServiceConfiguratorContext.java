/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Context for building singleton services.
 * @author Paul Ferraro
 */
public interface DistributedSingletonServiceConfiguratorContext {
    SupplierDependency<ServiceProviderRegistry<ServiceName>> getServiceProviderRegistryDependency();
    SupplierDependency<CommandDispatcherFactory> getCommandDispatcherFactoryDependency();
}
