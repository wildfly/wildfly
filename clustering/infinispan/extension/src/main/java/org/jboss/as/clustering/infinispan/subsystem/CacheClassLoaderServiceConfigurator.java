/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.infinispan.commons.util.AggregatedClassLoader;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Configures a service that provides the {@link ClassLoader} for a cache.
 * @author Paul Ferraro
 */
public enum CacheClassLoaderServiceConfigurator implements ResourceServiceConfigurator {
    INSTANCE;

    static final BinaryServiceDescriptor<ClassLoader> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of(InfinispanServiceDescriptor.CACHE_CONFIGURATION.getName() + ".loader", ClassLoader.class);

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        BinaryServiceConfiguration config = BinaryServiceConfiguration.of(address.getParent().getLastElement().getValue(), address.getLastElement().getValue());
        ServiceDependency<ClassLoader> loader = config.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION).combine(CacheResourceDefinitionRegistrar.MODULES.resolve(context, model), new BiFunction<>() {
            @Override
            public ClassLoader apply(GlobalConfiguration global, List<Module> modules) {
                if (modules.isEmpty()) {
                    return global.classLoader();
                }
                if (modules.size() == 1) {
                    return modules.get(0).getClassLoader();
                }
                return new AggregatedClassLoader(modules.stream().map(Module::getClassLoader).collect(Collectors.toUnmodifiableList()));
            }
        });
        return ServiceInstaller.builder(loader).provides(config.resolveServiceName(SERVICE_DESCRIPTOR)).build();
    }
}
