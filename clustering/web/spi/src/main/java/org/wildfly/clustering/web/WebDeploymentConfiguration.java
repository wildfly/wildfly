/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web;

import org.wildfly.clustering.ee.DeploymentConfiguration;

/**
 * Encapsulates the configuration of a web deployment.
 * @author Paul Ferraro
 */
public interface WebDeploymentConfiguration extends DeploymentConfiguration {

    /**
     * Returns the target server name of this deployment
     * @return a server name
     */
    String getServerName();
}
