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

package org.jboss.as.model;

/**
 * Add a deployment to the domain.
 *
 * @author Brian Stansberry
 */
public final class DomainDeploymentAdd extends AbstractDomainModelUpdate<Void> {

    private static final long serialVersionUID = -9076890219875153928L;

    private final String uniqueName;
    private final String runtimeName;
    private final byte[] hash;
    private final boolean start;

    /**
     * Construct a new instance.
     *
     * @param uniqueName the user-supplied unique name of the deployment
     * @param runtimeName the name by which the deployment should be known
     *                    in the runtime
     * @param hash the hash of the deployment
     * @param start whether the deployment should be started
     *              (i.e. deployed into the runtime) by default if
     *              it is mapped to a server
     *
     */
    public DomainDeploymentAdd(final String uniqueName, final String runtimeName, final byte[] hash, boolean start) {
        if (uniqueName == null)
            throw new IllegalArgumentException("uniqueName is null");
        if (runtimeName == null)
            throw new IllegalArgumentException("runtimeName is null");
        if (hash == null)
            throw new IllegalArgumentException("hash is null");
        this.uniqueName = uniqueName;
        this.runtimeName = runtimeName;
        this.hash = hash;
        this.start = start;
    }

    /**
     * Get the user-supplied unique name of the deployment.
     *
     * @return the unique name. Will not be {@code null}
     */
    public String getUniqueName() {
        return uniqueName;
    }

    /**
     * Gets the name by which the deployment should be known in the runtime.
     *
     * @return the runtime name. Will not be {@code null}
     */
    public String getRuntimeName() {
        return runtimeName;
    }

    /**
     * Gets the hash of the deployment.
     *
     * @return the hash. Will not be {@code null}
     */
    public byte[] getHash() {
        return hash;
    }

    /**
     * Gets whether the deployment should be started (i.e. deployed into the
     * runtime) by default if it is mapped to a server
     *
     * @return <code>true</code> if the deployment should be deployed by default
     */
    public boolean isStart() {
        return start;
    }


    @Override
    protected void applyUpdate(final DomainModel element) throws UpdateFailedException {
        DeploymentUnitElement due = new DeploymentUnitElement(uniqueName, runtimeName, hash, start);
        if (! element.addDeployment(due)) {
            throw new UpdateFailedException("Deployment '" + uniqueName + "' is already configured");
        }
    }

    @Override
    public DomainDeploymentRemove getCompensatingUpdate(final DomainModel original) {
        return new DomainDeploymentRemove(uniqueName);
    }

    @Override
    protected AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return null;
    }
}
