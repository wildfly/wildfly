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
* Update used when updating a deployment element to be started.
*
* @author Brian Stansberry
*/
public class ServerModelDeploymentReplaceUpdate extends AbstractServerModelUpdate<ServerDeploymentActionResult> {
    private static final long serialVersionUID = 5773083013951607950L;

    private final String newDeployment;
    private final String toReplace;
    private final ServerGroupDeploymentStartStopUpdate undeploymentModelUpdate;
    private final ServerGroupDeploymentStartStopUpdate deploymentModelUpdate;
    private final ServerDeploymentStartStopHandler startStopHandler;
    private ServerGroupDeploymentElement deploymentElement;

    public ServerModelDeploymentReplaceUpdate(final String newDeployment, final String toReplace) {
        if (newDeployment == null)
            throw new IllegalArgumentException("newDeployment is null");
        if (toReplace == null)
            throw new IllegalArgumentException("toReplace is null");
        this.newDeployment = newDeployment;
        this.toReplace = toReplace;
        undeploymentModelUpdate = new ServerGroupDeploymentStartStopUpdate(toReplace, false);
        deploymentModelUpdate = new ServerGroupDeploymentStartStopUpdate(newDeployment, true);
        startStopHandler = new ServerDeploymentStartStopHandler();
    }

    @Override
    public void applyUpdate(ServerModel standalone) throws UpdateFailedException {

        ServerGroupDeploymentElement undeploymentElement = standalone.getDeployment(toReplace);

        if (undeploymentElement == null) {
            throw new UpdateFailedException("Unknown deployment " + newDeployment);
        }

        deploymentElement = standalone.getDeployment(newDeployment);

        if (deploymentElement == null) {
            throw new UpdateFailedException("Unknown deployment " + newDeployment);
        }

        undeploymentModelUpdate.applyUpdate(undeploymentElement);
        deploymentModelUpdate.applyUpdate(deploymentElement);
    }

    @Override
    public <P> void applyUpdate(UpdateContext updateContext,
            UpdateResultHandler<? super ServerDeploymentActionResult, P> resultHandler, P param) {
        if (deploymentElement != null) {
            // FIXME coordinate results!!!
            startStopHandler.undeploy(toReplace, updateContext, resultHandler, param);
            startStopHandler.deploy(newDeployment, deploymentElement.getRuntimeName(), deploymentElement.getSha1Hash(),
                    updateContext, resultHandler, param);
        }
        else if (resultHandler != null) {
            // We shouldn't be able to get here, as the model update should have failed,
            // but just in case
            throw new IllegalStateException("Unknown deployment " + newDeployment);
        }
    }

    @Override
    public AbstractServerModelUpdate<?> getCompensatingUpdate(ServerModel original) {

        ServerGroupDeploymentElement deploymentElement = original.getDeployment(newDeployment);
        ServerGroupDeploymentElement undeploymentElement = original.getDeployment(toReplace);
        if (deploymentElement == null || undeploymentElement == null) {
            // We will fail in applyUpdate and won't do anything, so don't
            // provide a compensating update that actually would do something
            return null;
        }
        return new ServerModelDeploymentReplaceUpdate(toReplace, newDeployment);
    }
}
