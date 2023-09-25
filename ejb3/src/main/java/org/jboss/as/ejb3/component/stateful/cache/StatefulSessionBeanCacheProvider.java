/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Provides configurators for services to install a stateful session bean cache factory.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface StatefulSessionBeanCacheProvider<K, V extends StatefulSessionBeanInstance<K>> {
    /**
     * Returns configurators for services to be installed for the specified deployment.
     * @param unit a deployment unit
     * @return a collection of service configurators
     */
    Iterable<CapabilityServiceConfigurator> getDeploymentServiceConfigurators(DeploymentUnit unit, EEModuleConfiguration moduleConfiguration);

    /**
     * Returns a configurator for a service supplying a cache factory.
     * @param unit the deployment unit containing this EJB component.
     * @param description the EJB component description
     * @param configuration the component configuration
     * @return a service configurator
     */
    CapabilityServiceConfigurator getStatefulBeanCacheFactoryServiceConfigurator(DeploymentUnit unit, StatefulComponentDescription description, ComponentConfiguration configuration);

    /**
     * Indicates whether or not cache factories provides by this object can support passivation.
     * @return true, if passivation is supported, false otherwise.
     */
    boolean supportsPassivation();
}
