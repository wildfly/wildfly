/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.metadata.deployment;

import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.msc.service.ServiceName;

/**
 * A resource adapter deployment
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class ResourceAdapterDeployment {

    private final CommonDeployment deployment;

    private final String raName;

    private final ServiceName raServiceName;

    /**
     * Create an instance
     * @param deployment The deployment
     * @param raName the resource-adapter name
     * @param raServiceName service name for this resource-adapter
     */
    public ResourceAdapterDeployment(final CommonDeployment deployment, final String raName, final ServiceName raServiceName) {
        this.deployment = deployment;
        this.raName = raName;
        this.raServiceName = raServiceName;
    }

    /**
     * Get the deployment
     * @return The deployment
     */
    public CommonDeployment getDeployment() {
        return deployment;
    }

    /**
     * String representation
     * @return The string
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(100);

        sb.append("ResourceAdapterDeployment@").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append("[deployment=").append(deployment);
        sb.append("]");

        return sb.toString();
    }

    public String getRaName() {
        return raName;
    }

    public ServiceName getRaServiceName() {
        return raServiceName;
    }

}
