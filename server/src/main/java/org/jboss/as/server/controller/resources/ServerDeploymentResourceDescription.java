/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.server.controller.resources;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.deployment.DeploymentAddHandler;
import org.jboss.as.server.deployment.DeploymentDeployHandler;
import org.jboss.as.server.deployment.DeploymentRedeployHandler;
import org.jboss.as.server.deployment.DeploymentRemoveHandler;
import org.jboss.as.server.deployment.DeploymentUndeployHandler;
import org.jboss.as.server.services.security.AbstractVaultReader;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServerDeploymentResourceDescription extends DeploymentResourceDescription {

    private final AbstractVaultReader vaultReader;

    private ServerDeploymentResourceDescription(AbstractVaultReader vaultReader, OperationStepHandler addHandler, OperationStepHandler removeHandler) {
        super(DeploymentResourceParent.SERVER, addHandler, removeHandler);
        this.vaultReader = vaultReader;
    }

    public static ServerDeploymentResourceDescription create(ContentRepository contentRepository, AbstractVaultReader vaultReader) {
        return new ServerDeploymentResourceDescription(vaultReader,
                DeploymentAddHandler.create(contentRepository, vaultReader),
                new DeploymentRemoveHandler(contentRepository, vaultReader));
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(DeploymentAttributes.DEPLOY_DEFINITION, new DeploymentDeployHandler(vaultReader));
        resourceRegistration.registerOperationHandler(DeploymentAttributes.UNDEPLOY_DEFINITION, new DeploymentUndeployHandler(vaultReader));
        resourceRegistration.registerOperationHandler(DeploymentAttributes.REDEPLOY_DEFINITION, new DeploymentRedeployHandler(vaultReader));
    }

    @Override
    protected void registerAddOperation(ManagementResourceRegistration registration, OperationStepHandler handler, Flag... flags) {
        registration.registerOperationHandler(DeploymentAttributes.SERVER_DEPLOYMENT_ADD_DEFINITION, handler);
    }
}
