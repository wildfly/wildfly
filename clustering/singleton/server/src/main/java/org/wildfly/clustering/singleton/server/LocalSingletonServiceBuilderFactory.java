/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.server;

import org.jboss.as.clustering.msc.InjectedValueDependency;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.singleton.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;

/**
 * Factory for creating local {@link SingletonServiceBuilder} instances.
 * @author Paul Ferraro
 */
@SuppressWarnings({ "removal", "deprecation" })
public class LocalSingletonServiceBuilderFactory extends LocalSingletonServiceConfiguratorFactory implements SingletonServiceBuilderFactory {

    private final LegacyLocalSingletonServiceConfiguratorContext context;

    public LocalSingletonServiceBuilderFactory(LocalSingletonServiceConfiguratorFactoryContext context) {
        super(context);
        this.context = new LegacyLocalSingletonServiceConfiguratorContext(context);
    }

    @Override
    public <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> service) {
        return new LocalSingletonServiceBuilder<>(name, service, this.context);
    }

    @Override
    public <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> primaryService, Service<T> backupService) {
        return this.createSingletonServiceBuilder(name, primaryService);
    }

    private class LegacyLocalSingletonServiceConfiguratorContext implements LocalSingletonServiceConfiguratorContext {
        private final LocalSingletonServiceConfiguratorFactoryContext context;

        LegacyLocalSingletonServiceConfiguratorContext(LocalSingletonServiceConfiguratorFactoryContext context) {
            this.context = context;
        }

        @Override
        public SupplierDependency<Group> getGroupDependency() {
            return new InjectedValueDependency<>(this.context.getGroupServiceName(), Group.class);
        }
    }
}
