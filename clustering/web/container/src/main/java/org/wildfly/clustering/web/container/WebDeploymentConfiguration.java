/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.container;

import org.jboss.modules.Module;

/**
 * Defines the configuration of a web deployment.
 * @author Paul Ferraro
 */
public interface WebDeploymentConfiguration {

    /**
     * Returns the target server name of this deployment
     * @return a server name
     */
    String getServerName();

    /**
     * Returns the name of this deployment
     * @return a deployment name
     */
    String getDeploymentName();

    /**
     * Returns the deployment module
     * @return the deployment module
     */
    Module getModule();
}
