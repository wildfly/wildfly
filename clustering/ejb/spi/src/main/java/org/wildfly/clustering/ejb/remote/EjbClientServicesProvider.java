/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.remote;

import org.jboss.as.network.ClientMapping;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

import java.util.List;

/**
 * interface defining EjbClientServicesProvider instances, used to install configured EJB client services.
 *
 * @author Paul Ferraro
 */
public interface EjbClientServicesProvider {
    NullaryServiceDescriptor<EjbClientServicesProvider> SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.ejb.ejb-client-services-provider", EjbClientServicesProvider.class);

    // TODO: where should these ServiceDescriptors live? This one assigns an alias which is set up in the installer
    NullaryServiceDescriptor<ServiceProviderRegistrar<Object, GroupMember>> MODULE_AVAILABILITY_REGISTRAR_SERVICE_PROVIDER_REGISTRAR = NullaryServiceDescriptor.of("org.wildfly.ejb.remote.module-availability-registrar-service-provider-registrar", (Class<ServiceProviderRegistrar<Object, GroupMember>>) (Class<?>) ServiceProviderRegistrar.class);

    Iterable<ServiceInstaller> getClientMappingsRegistryServiceInstallers(String connectorName, ServiceDependency<List<ClientMapping>> clientMappings);
    Iterable<ServiceInstaller> getModuleAvailabilityRegistrarServiceInstallers();
}
