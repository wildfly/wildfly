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
import org.jboss.as.model.ServerGroupDeploymentElement;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;


/**
* Update used when updating a deployment element to be started or stopped.
*
* @author John E. Bailey
* @author Brian Stansberry
*/
public class ServerModelDeploymentStopUpdate extends AbstractServerModelUpdate<Void> {
    private static final long serialVersionUID = 5773083013951607950L;

    private ServerGroupDeploymentElement deploymentElement;
    private final String deploymentUnitName;

    public ServerModelDeploymentStopUpdate(final String deploymentUnitName) {
        super(false, true);
        if (deploymentUnitName == null)
            throw new IllegalArgumentException("deploymentUnitName is null");
        this.deploymentUnitName = deploymentUnitName;
    }

    public String getDeploymentUnitName() {
        return deploymentUnitName;
    }

    @Override
    public ServerModelDeploymentStartUpdate getCompensatingUpdate(ServerModel original) {
        ServerGroupDeploymentElement element = original.getDeployment(getDeploymentUnitName());
        if (element == null) {
            return null;
        }
        return new ServerModelDeploymentStartUpdate(deploymentUnitName);
    }

    @Override
    public void applyUpdate(ServerModel serverModel) throws UpdateFailedException {
        // TODO caching the deploymentElement for use in the runtime update
        // has a bad smell
        deploymentElement = serverModel.getDeployment(getDeploymentUnitName());
        if (deploymentElement != null) {
            deploymentElement.setStart(false);
        }
    }

    @Override
    public <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
        // TODO using the deploymentElement cached in the model update method
        // has a bad smell
        final ServiceName deploymentUnitServiceName = Services.JBOSS_DEPLOYMENT.append(deploymentUnitName);
        final ServiceRegistry serviceRegistry = updateContext.getServiceRegistry();

        if (deploymentElement != null) {
            final ServiceController<?> controller = serviceRegistry.getService(deploymentUnitServiceName);
            controller.setMode(ServiceController.Mode.NEVER);
            // TODO - connect to service lifecycle properly
            if (resultHandler != null)
                resultHandler.handleSuccess(null, param);
        }
        else if (resultHandler != null) {
            resultHandler.handleSuccess(null, param);
        }
    }
}
