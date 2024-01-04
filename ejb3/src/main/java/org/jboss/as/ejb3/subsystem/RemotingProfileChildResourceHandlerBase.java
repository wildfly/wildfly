/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentResourceHandlerBase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

public abstract class RemotingProfileChildResourceHandlerBase extends RestartParentResourceHandlerBase {

     protected RemotingProfileChildResourceHandlerBase() {
         super(EJB3SubsystemModel.REMOTING_PROFILE);
    }

    @Override
    protected void recreateParentService(final OperationContext context, final PathAddress parentAddress, final ModelNode parentModel) throws OperationFailedException {

        switch(context.getCurrentStage()) {
            case RUNTIME:
                // service installation in another step: when interruption is thrown then it is handled by RollbackHandler
                // declared in RestartParentResourceHandlerBase
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        RemotingProfileResourceDefinition.ADD_HANDLER.installServices(context, parentAddress, parentModel);
                    }
                }, OperationContext.Stage.RUNTIME);
            break;
            case DONE:
                // executed from RollbackHandler - service is being installed using correct configuration
                RemotingProfileResourceDefinition.ADD_HANDLER.installServices(context, parentAddress, parentModel);
                break;
        }
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
       return RemotingProfileResourceDefinition.REMOTING_PROFILE_CAPABILITY.getCapabilityServiceName(parentAddress);
    }

    @Override
    protected void removeServices(OperationContext context, ServiceName parentService, ModelNode parentModel) throws OperationFailedException {
        super.removeServices(context, parentService, parentModel);
    }
}
