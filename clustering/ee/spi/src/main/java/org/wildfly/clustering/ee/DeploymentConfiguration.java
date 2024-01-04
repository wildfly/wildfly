/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee;

/**
 * Encapsulates the configuration of a deployment.
 * @author Paul Ferraro
 */
public interface DeploymentConfiguration {

    /**
     * Returns the locally unique name of this deployment.
     * @return a deployment name.
     */
    String getDeploymentName();
}
