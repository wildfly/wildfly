/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.bean;

import org.jboss.msc.service.ServiceName;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Provides service installation mechanics for components of bean deployments.
 * @author Paul Ferraro
 */
public interface BeanManagementProvider {
    NullaryServiceDescriptor<BeanManagementProvider> DEFAULT_SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.ejb.default-bean-management-provider", BeanManagementProvider.class);
    UnaryServiceDescriptor<BeanManagementProvider> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.ejb.bean-management-provider", DEFAULT_SERVICE_DESCRIPTOR);

    /**
     * Returns a name uniquely identifying this provider.
     * @return the provider name
     */
    String getName();

    /**
     * Installs dependencies for a deployment unit
     * @param configuration a bean deployment configuration
     * @return a collection of service configurators
     */
    Iterable<ServiceInstaller> getDeploymentServiceInstallers(BeanDeploymentConfiguration configuration);

    /**
     * Builds a bean manager factory for a Jakarta Enterprise Beans bean within a deployment.
     * @param configuration a bean configuration
     * @return a service configurator
     */
    ServiceInstaller getBeanManagerFactoryServiceInstaller(ServiceName name, BeanConfiguration configuration);
}
