/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.container;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.msc.service.ServiceName;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;

/**
 * Container-specific session management provider for a deployment.
 * @author Paul Ferraro
 */
public interface SessionManagementProvider {

    /**
     * Returns an installer of a service providing a container-specific session manager factory.
     * @param name the service name of the session manager factory service
     * @param configuration the configuration of the session manager factory
     * @return a number of service configurators
     */
    DeploymentServiceInstaller getSessionManagerFactoryServiceInstaller(ServiceName name, SessionManagerFactoryConfiguration configuration);

    /**
     * Returns an installer of a service providing container-specific session affinity logic.
     * @param context the deployment phase context
     * @param name the service name of the session affinity service
     * @param configuration the configuration of the deployment
     * @return a number of service configurators
     */
    DeploymentServiceInstaller getSessionAffinityServiceInstaller(DeploymentPhaseContext context, ServiceName name, WebDeploymentConfiguration configuration);
}
