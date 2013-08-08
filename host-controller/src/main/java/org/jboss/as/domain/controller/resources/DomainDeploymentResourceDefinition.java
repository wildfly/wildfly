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
package org.jboss.as.domain.controller.resources;

import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.domain.controller.operations.deployment.DeploymentAddHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentRemoveHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentAddHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentDeployHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentRedeployHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentRemoveHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentUndeployHandler;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.controller.resources.DeploymentAttributes;
import org.jboss.as.server.controller.resources.DeploymentResourceDefinition;
 class DomainDeploymentResourceDefinition extends DeploymentResourceDefinition {

    private OperationDefinition addDefinition;
    private DomainDeploymentResourceDefinition(DeploymentResourceParent parent, OperationDefinition addDefinition, OperationStepHandler addHandler, OperationStepHandler removeHandler) {
        super(parent, addHandler, removeHandler);
        this.addDefinition = addDefinition;
    }

    public static DomainDeploymentResourceDefinition createForDomainRoot(boolean isMaster, ContentRepository contentRepository, HostFileRepository fileRepository) {
        return new DomainDeploymentResourceDefinition(DeploymentResourceParent.DOMAIN,
                DeploymentAttributes.DOMAIN_DEPLOYMENT_ADD_DEFINITION,
                isMaster ? new DeploymentAddHandler(contentRepository) : new DeploymentAddHandler(),
                isMaster ? DeploymentRemoveHandler.createForMaster(contentRepository) : DeploymentRemoveHandler.createForSlave(fileRepository));
    }

    public static DomainDeploymentResourceDefinition createForServerGroup(ContentRepository contentRepository, HostFileRepository fileRepository) {
        return new DomainDeploymentResourceDefinition(DeploymentResourceParent.SERVER_GROUP, DeploymentAttributes.SERVER_GROUP_DEPLOYMENT_ADD_DEFINITION,
                new ServerGroupDeploymentAddHandler(fileRepository), ServerGroupDeploymentRemoveHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        if (getParent() == DeploymentResourceParent.SERVER_GROUP) {
            resourceRegistration.registerOperationHandler(DeploymentAttributes.DEPLOY_DEFINITION, ServerGroupDeploymentDeployHandler.INSTANCE);
            resourceRegistration.registerOperationHandler(DeploymentAttributes.REDEPLOY_DEFINITION, ServerGroupDeploymentRedeployHandler.INSTANCE);
            resourceRegistration.registerOperationHandler(DeploymentAttributes.UNDEPLOY_DEFINITION, ServerGroupDeploymentUndeployHandler.INSTANCE);
        }
    }

     @Override
    protected void registerAddOperation(ManagementResourceRegistration registration, OperationStepHandler handler, Flag... flags) {
        registration.registerOperationHandler(addDefinition, handler);
    }

}
