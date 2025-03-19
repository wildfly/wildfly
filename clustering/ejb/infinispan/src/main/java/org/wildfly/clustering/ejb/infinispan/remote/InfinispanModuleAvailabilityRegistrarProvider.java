/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.remote;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ejb.remote.ModuleAvailabilityRegistrarProvider;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The non-legacy version of the module availability registrar provider, used when the distributable-ejb subsystem is present.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanModuleAvailabilityRegistrarProvider implements ModuleAvailabilityRegistrarProvider {

    private final BinaryServiceConfiguration configuration;
    private final Consumer<ConfigurationBuilder> configurator;

    /**
     * Creates an instance of the Infinispan-based service provider registrar provider.
     * @param configuration a cache configuration
     */
    public InfinispanModuleAvailabilityRegistrarProvider(BinaryServiceConfiguration configuration) {
        this(configuration, Functions.discardingConsumer());
    }

    InfinispanModuleAvailabilityRegistrarProvider(BinaryServiceConfiguration configuration, Consumer<ConfigurationBuilder> configurator) {
        this.configuration = configuration;
        this.configurator = configurator;
    }

    /**
     * This method aliases the ServiceProviderRegistry defined for the "module-availability" cache for the "ejb" container
     * to the capability for the ServiceProviderRegistry used by the ModuleAvailabilityRegistrar instance.
     * @param support
     * @return
     */
    @Override
    public Iterable<ServiceInstaller> getServiceInstallers(CapabilityServiceSupport support) {
        List<ServiceInstaller> installers = new LinkedList<>();
        // get a handle to the existing ServiceProviderRegistry installed for the cache
        ServiceName serviceProviderRegistrarServiceName = configuration.resolveServiceName(ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR);
        ServiceDependency<ServiceProviderRegistrar<Object, GroupMember>> serviceProviderRegistrar = ServiceDependency.on(serviceProviderRegistrarServiceName);
        // create an installer to install a well-known alias to that SPR
        ServiceName aliasServiceName = ServiceName.of(ModuleAvailabilityRegistrarProvider.MODULE_AVAILABILITY_REGISTRAR_SERVICE_PROVIDER_REGISTRAR.getName());
        installers.add(ServiceInstaller.builder(serviceProviderRegistrar).provides(aliasServiceName).build());
        return installers;
    }
}
