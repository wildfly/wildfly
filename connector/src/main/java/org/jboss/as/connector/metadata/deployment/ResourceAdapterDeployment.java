/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
