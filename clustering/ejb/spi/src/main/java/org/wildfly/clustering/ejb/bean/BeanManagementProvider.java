/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.bean;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;

/**
 * Provides service installation mechanics for components of bean deployments.
 * @author Paul Ferraro
 */
public interface BeanManagementProvider {

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
    Iterable<CapabilityServiceConfigurator> getDeploymentServiceConfigurators(BeanDeploymentConfiguration configuration);

    /**
     * Builds a bean manager factory for an Jakarta Enterprise Bean within a deployment.
     * @param configuration a bean configuration
     * @return a service configurator
     */
    CapabilityServiceConfigurator getBeanManagerFactoryServiceConfigurator(BeanConfiguration configuration);
}
