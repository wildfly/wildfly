/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.singleton.service.SingletonServiceConfigurator;
import org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory;

/**
 * Factory for creating local {@link SingletonServiceConfigurator} instances.
 * @author Paul Ferraro
 */
public class LocalSingletonServiceConfiguratorFactory implements SingletonServiceConfiguratorFactory, LocalSingletonServiceConfiguratorContext {

    private final LocalSingletonServiceConfiguratorFactoryContext context;

    public LocalSingletonServiceConfiguratorFactory(LocalSingletonServiceConfiguratorFactoryContext context) {
        this.context = context;
    }

    @Override
    public SingletonServiceConfigurator createSingletonServiceConfigurator(ServiceName name) {
        return new LocalSingletonServiceConfigurator(name, this);
    }

    @Override
    public SupplierDependency<Group> getGroupDependency() {
        return new ServiceSupplierDependency<>(this.context.getGroupServiceName());
    }
}
