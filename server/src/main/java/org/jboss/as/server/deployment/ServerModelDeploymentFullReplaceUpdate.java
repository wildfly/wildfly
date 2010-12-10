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
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;


/**
* Update used when updating a deployment element to be started.
*
* @author Brian Stansberry
*/
public class ServerModelDeploymentFullReplaceUpdate extends AbstractServerModelUpdate<Void> {
    private static final long serialVersionUID = 5773083013951607950L;
//    private static final Logger log = Logger.getLogger("org.jboss.as.model");

    private final String deploymentUniqueName;
    private final String deploymentRuntimeName;
    private final byte[] hash;
    private final boolean redeploy;

    public ServerModelDeploymentFullReplaceUpdate(final String deploymentUniqueName, final String deploymentRuntimeName, final byte[] hash, final boolean redeploy) {
        super(false, true);
        if (deploymentUniqueName == null)
            throw new IllegalStateException("deploymentUniqueName is null");
        if (deploymentRuntimeName == null)
            throw new IllegalStateException("deploymentRuntimeName is null");
        if (hash == null)
            throw new IllegalStateException("hash is null");
        this.deploymentUniqueName = deploymentUniqueName;
        this.deploymentRuntimeName = deploymentRuntimeName;
        this.hash = hash;
        this.redeploy = redeploy;
    }

    @Override
    public void applyUpdate(ServerModel element) throws UpdateFailedException {
        ServerGroupDeploymentElement toRemove = element.getDeployment(deploymentUniqueName);
        if (toRemove != null) {
            element.removeDeployment(deploymentUniqueName);
        }
        element.addDeployment(new ServerGroupDeploymentElement(deploymentUniqueName, deploymentRuntimeName, hash, redeploy));
    }

    @Override
    public <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
        if (redeploy) {
            final ServiceController<?> controller = updateContext.getServiceRegistry().getService(Services.JBOSS_DEPLOYMENT_UNIT.append(deploymentUniqueName));
            if(controller != null) {
                controller.addListener(new AbstractServiceListener<Object>() {
                    public void serviceRemoved(ServiceController<? extends Object> serviceController) {
                        deploy(updateContext);
                    }
                });
                controller.setMode(ServiceController.Mode.REMOVE);
            } else {
                deploy(updateContext);
            }
        }
        else if (resultHandler != null) {
            resultHandler.handleSuccess(null, param);
        }
    }

    private void deploy(final UpdateContext updateContext) {
        final ServiceTarget serviceTarget = updateContext.getServiceTarget();
        final DeploymentUnitService service = new DeploymentUnitService(deploymentUniqueName,  null);
        serviceTarget.addService(Services.JBOSS_DEPLOYMENT_UNIT.append(deploymentUniqueName), service)
            .addDependency(Services.JBOSS_DEPLOYMENT_CHAINS, DeployerChains.class, service.getDeployerChainsInjector())
            .setInitialMode(ServiceController.Mode.ACTIVE);
    }

    @Override
    public ServerModelDeploymentFullReplaceUpdate getCompensatingUpdate(ServerModel original) {
        ServerGroupDeploymentElement deployment = original.getDeployment(deploymentUniqueName);
        if (deployment == null) {
            return null;
        }
        return new ServerModelDeploymentFullReplaceUpdate(deploymentUniqueName, deployment.getRuntimeName(), deployment.getSha1Hash(), redeploy);
    }
}
