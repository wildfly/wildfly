/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.container;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;

/**
 * Container-specific session management provider for a deployment.
 * @author Paul Ferraro
 */
public interface SessionManagementProvider {

    /**
     * Returns a set of configurators for services providing a container-specific session manager factory.
     * @param name the service name of the session manager factory service
     * @param configuration the configuration of the session manager factory
     * @return a number of service configurators
     */
    Iterable<CapabilityServiceConfigurator> getSessionManagerFactoryServiceConfigurators(ServiceName name, SessionManagerFactoryConfiguration configuration);

    /**
     * Returns set of configurators for services providing container-specific session affinity logic.
     * @param name the service name of the session affinity service
     * @param configuration the configuration of the deployment
     * @return a number of service configurators
     */
    Iterable<CapabilityServiceConfigurator> getSessionAffinityServiceConfigurators(ServiceName name, WebDeploymentConfiguration configuration);
}
