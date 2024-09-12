/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.registry;

import java.util.function.BiFunction;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.BinaryServiceInstallerFactory;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Configures a cache or local registry factory.
 * @author Paul Ferraro
 */
@MetaInfServices(BinaryServiceInstallerFactory.class)
public class RegistryFactoryServiceInstallerFactory<K, V> extends AbstractRegistryFactoryServiceInstallerFactory<K, V> {

    @Override
    public ServiceInstaller apply(CapabilityServiceSupport support, BinaryServiceConfiguration configuration) {
        BiFunction<CapabilityServiceSupport, BinaryServiceConfiguration, ServiceInstaller> factory = configuration.getParentName().equals(ModelDescriptionConstants.LOCAL) ? new LocalRegistryFactoryServiceInstallerFactory<>() : new CacheRegistryFactoryServiceInstallerFactory<>();
        return factory.apply(support, configuration);
/*
        ServiceDependency<Group<GroupMember>> group = configuration.getServiceDependency(ClusteringServiceDescriptor.GROUP);
        return ServiceInstaller.builder(new ServiceInstaller() {
            @Override
            public ServiceController<?> install(RequirementServiceTarget target) {
                BiFunction<CapabilityServiceSupport, BinaryServiceConfiguration, ServiceInstaller> factory = group.get().isSingleton() ? new LocalRegistryFactoryServiceInstallerFactory<>() : new CacheRegistryFactoryServiceInstallerFactory<>();
                return factory.apply(support, configuration).install(target);
            }
        }, support).requires(group).build();
*/
    }
}
