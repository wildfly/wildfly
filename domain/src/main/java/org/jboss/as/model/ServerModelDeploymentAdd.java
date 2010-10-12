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

import java.util.Arrays;

import org.jboss.as.deployment.client.api.server.ServerDeploymentActionResult;

/**
 * Update to the ServerModel element to add a new deployment.
 *
 * @author John E. Bailey
 * @author Brian Stansberry
 */
public class ServerModelDeploymentAdd extends AbstractServerModelUpdate<ServerDeploymentActionResult> {

    private static final long serialVersionUID = -5804608026829597800L;

    private final String deploymentUniqueName;
    private final String deploymentRuntimeName;
    private final byte[] deploymentHash;
    private final ServerDeploymentStartStopHandler startStopHandler;

    public ServerModelDeploymentAdd(final String deploymentUniqueName, final String deploymentRuntimeName, final byte[] deploymentHash,
            boolean deploy) {
        super(false, true);
        if (deploymentUniqueName == null)
            throw new IllegalArgumentException("deploymentUniqueName is null");
        if (deploymentRuntimeName == null)
            throw new IllegalArgumentException("deploymentRuntimeName is null");
        if (deploymentHash == null)
            throw new IllegalArgumentException("deploymentHash is null");
        this.deploymentUniqueName = deploymentUniqueName;
        this.deploymentRuntimeName = deploymentRuntimeName;
        this.deploymentHash = deploymentHash;
        if (deploy) {
            startStopHandler = new ServerDeploymentStartStopHandler();
        }
        else {
            startStopHandler = null;
        }
    }

    @Override
    public ServerModelDeploymentRemove getCompensatingUpdate(ServerModel original) {
        return new ServerModelDeploymentRemove(deploymentUniqueName, startStopHandler != null);
    }

    @Override
    public void applyUpdate(ServerModel serverModel) throws UpdateFailedException {
        ServerGroupDeploymentElement existing = serverModel.getDeployment(deploymentUniqueName);
        if (existing != null) {
            if (!Arrays.equals(existing.getSha1Hash(), deploymentHash)) {
                throw new UpdateFailedException("Deployment content with name " + deploymentUniqueName + " and hash " + existing.getSha1HashAsHexString() + "already ");
            }
        }
        else {
            serverModel.addDeployment(new ServerGroupDeploymentElement(deploymentUniqueName, deploymentRuntimeName, deploymentHash, false));
        }
    }

    @Override
    public <P> void applyUpdate(UpdateContext updateContext,
            UpdateResultHandler<? super ServerDeploymentActionResult, P> resultHandler, P param) {
        if (startStopHandler != null) {
            startStopHandler.deploy(deploymentUniqueName, deploymentRuntimeName, deploymentHash, updateContext.getBatchBuilder(), updateContext.getServiceContainer(), resultHandler, param);
        }
    }
}
