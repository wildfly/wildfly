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

package org.jboss.as.server.deployment;

import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.Attribute;
import org.jboss.as.model.ServerGroupDeploymentElement;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;

/**
 * Update to a standalone element to remove a deployment.
 *
 * @author John E. Bailey
 * @author Brian Stansberry
 */
public class ServerModelDeploymentRemove extends AbstractServerModelUpdate<Void> {
    private static final long serialVersionUID = -3612085673646053177L;

    private final String deploymentName;

    public ServerModelDeploymentRemove(final String deploymentName) {
        super(false, true);
        this.deploymentName = deploymentName;
    }

    @Override
    public void applyUpdate(ServerModel standalone) throws UpdateFailedException {
        ServerGroupDeploymentElement toRemove = standalone.getDeployment(deploymentName);
        if (toRemove != null) {
            if (toRemove.isStart()) {
                throw new UpdateFailedException("Cannot remove deployment " +
                        deploymentName + " as its " + Attribute.START +
                        " attribute is 'true'. Deployment must be undeployed before removal.");
            }
            standalone.removeDeployment(deploymentName);
        }
    }

    @Override
    public <P> void applyUpdate(UpdateContext updateContext,
            UpdateResultHandler<? super Void, P> resultHandler, P param) {
        if (resultHandler != null) {
            resultHandler.handleSuccess(null, param);
        }
    }

    @Override
    public ServerModelDeploymentAdd getCompensatingUpdate(ServerModel original) {
        ServerGroupDeploymentElement toRemove = original.getDeployment(deploymentName);
        if (toRemove == null) {
            return null;
        }
        return new ServerModelDeploymentAdd(toRemove.getUniqueName(), toRemove.getRuntimeName(), toRemove.getSha1Hash());
    }
}
