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

import org.jboss.as.deployment.client.api.server.ServerDeploymentActionResult;


/**
 * Update used when replacing a deployment in a server group with another deployment.
 * Adds a ServerGroupDeploymentElement to the server group and
 *
 * @author Brian Stansberry
 */
public class ServerGroupDeploymentReplaceUpdate extends AbstractModelUpdate<ServerGroupElement, ServerDeploymentActionResult> {
    private static final long serialVersionUID = 5773083013951607950L;

    private final String uniqueName;
    private final String runtimeName;
    private final byte[] hash;
    private final String replacedDeploymentName;

    public ServerGroupDeploymentReplaceUpdate(final String uniqueName, final String runtimeName, final byte[] hash, final String replacedDeploymentName) {
        if (uniqueName == null)
            throw new IllegalArgumentException("uniqueName is null");
        if (runtimeName == null)
            throw new IllegalArgumentException("runtimeName is null");
        if (hash == null)
            throw new IllegalArgumentException("hash is null");
        if (replacedDeploymentName == null)
            throw new IllegalArgumentException("replacedDeploymentName is null");
        this.uniqueName = uniqueName;
        this.runtimeName = runtimeName;
        this.hash = hash;
        this.replacedDeploymentName = replacedDeploymentName;
    }

    @Override
    public ServerGroupDeploymentReplaceUpdate getCompensatingUpdate(ServerGroupElement original) {
        ServerGroupDeploymentElement toReplace = original.getDeployment(replacedDeploymentName);
        if (toReplace == null)
            return null;
        return new ServerGroupDeploymentReplaceUpdate(toReplace.getUniqueName(), toReplace.getRuntimeName(), toReplace.getSha1Hash(), uniqueName);
    }

    @Override
    protected ServerModelDeploymentReplaceUpdate getServerModelUpdate() {
        return new ServerModelDeploymentReplaceUpdate(uniqueName, runtimeName, hash, replacedDeploymentName);
    }

    @Override
    protected void applyUpdate(ServerGroupElement serverGroupElement) throws UpdateFailedException {

        ServerGroupDeploymentElement undeploymentElement = serverGroupElement.getDeployment(replacedDeploymentName);

        if (undeploymentElement == null) {
            throw new UpdateFailedException("Unknown deployment " + serverGroupElement);
        }

        // This may also be an add
        serverGroupElement.addDeployment(uniqueName, runtimeName, hash, false);
        ServerGroupDeploymentElement deploymentElement = serverGroupElement.getDeployment(uniqueName);

        undeploymentElement.setStart(false);
        deploymentElement.setStart(true);
    }

    @Override
    public Class<ServerGroupElement> getModelElementType() {
        return ServerGroupElement.class;
    }
}
