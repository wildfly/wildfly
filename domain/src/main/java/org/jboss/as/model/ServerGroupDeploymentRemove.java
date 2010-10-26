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
 * Update used when removing a deployment element from a ServerGroup.
 *
 * @author Brian Stansberry
 */
public class ServerGroupDeploymentRemove extends AbstractModelUpdate<ServerGroupElement, Void> {
    private static final long serialVersionUID = 5773083013951607950L;

    private final String uniqueName;

    /**
     * Construct a new instance.
     *
     * @param uniqueName the user-supplied unique name of the deployment
     *
     */
    public ServerGroupDeploymentRemove(final String uniqueName) {
        if (uniqueName == null)
            throw new IllegalArgumentException("uniqueName is null");
        this.uniqueName = uniqueName;
    }

    /**
     * Get the user-supplied unique name of the deployment.
     *
     * @return the unique name. Will not be {@code null}
     */
    public String getUniqueName() {
        return uniqueName;
    }

    @Override
    public ServerGroupDeploymentAdd getCompensatingUpdate(ServerGroupElement original) {
        ServerGroupDeploymentElement dep = original.getDeployment(uniqueName);
        return dep == null ? null : new ServerGroupDeploymentAdd(uniqueName, dep.getRuntimeName(), dep.getSha1Hash(), dep.isStart());
    }

    @Override
    protected ServerModelDeploymentRemove getServerModelUpdate() {
        return new ServerModelDeploymentRemove(uniqueName);
    }

    @Override
    protected void applyUpdate(ServerGroupElement element) throws UpdateFailedException {
        if (element.getDeployment(uniqueName) != null) {
            throw new UpdateFailedException("Deployment " + uniqueName + " already added");
        }
        element.removeDeployment(uniqueName);
    }

    @Override
    public Class<ServerGroupElement> getModelElementType() {
        return ServerGroupElement.class;
    }
}
