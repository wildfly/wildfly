/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.remote;

import org.jboss.as.network.ClientMapping;
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

    Iterable<ServiceInstaller> getServiceInstallers(String connectorName, ServiceDependency<List<ClientMapping>> clientMappings);
}
