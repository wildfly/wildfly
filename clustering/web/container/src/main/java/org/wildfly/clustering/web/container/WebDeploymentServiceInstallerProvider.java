/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.container;

import org.wildfly.subsystem.service.DeploymentServiceInstaller;

/**
 * Provides service installers for a web deployment.
 * @author Paul Ferraro
 */
public interface WebDeploymentServiceInstallerProvider {

    /**
     * Returns an installer of a service providing a container-specific session manager factory.
     * @param name the service name of the session manager factory service
     * @param configuration the configuration of the session manager factory
     * @return a number of service configurators
     */
    DeploymentServiceInstaller getSessionManagerFactoryServiceInstaller(SessionManagerFactoryConfiguration configuration);

    /**
     * Returns an installer of a service providing container-specific session affinity logic.
     * @param context the deployment phase context
     * @param name the service name of the session affinity service
     * @param configuration the configuration of the deployment
     * @return a number of service configurators
     */
    DeploymentServiceInstaller getSessionAffinityProviderServiceInstaller(WebDeploymentConfiguration configuration);
}
