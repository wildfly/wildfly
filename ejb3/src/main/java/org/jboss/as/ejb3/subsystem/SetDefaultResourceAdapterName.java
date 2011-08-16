/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.component.messagedriven.DefaultResourceAdapterService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * User: jpai
 */
public class SetDefaultResourceAdapterName implements OperationStepHandler {

    public static final SetDefaultResourceAdapterName INSTANCE = new SetDefaultResourceAdapterName();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        // get the resource adapter name value from the operation's "value" param
        final String resourceAdapterName = operation.require(ModelDescriptionConstants.VALUE).asString();
        // update the model
        // first get the ModelNode for the address on which this operation was executed. i.e. /subsystem=ejb3
        ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        model.get(EJB3SubsystemModel.DEFAULT_RESOURCE_ADAPTER_NAME).set(resourceAdapterName);

        // now create a runtime operation to update the default resource adapter name used by the MDBs
        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new DefaultResourceAdapterNameUpdateHandler(resourceAdapterName), OperationContext.Stage.RUNTIME);
        }

        // complete the step
        context.completeStep();
    }

    private class DefaultResourceAdapterNameUpdateHandler implements OperationStepHandler {

        private final String resourceAdapterName;

        DefaultResourceAdapterNameUpdateHandler(final String resourceAdapterName) {
            this.resourceAdapterName = resourceAdapterName;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
            ServiceController<DefaultResourceAdapterService> existingDefaultRANameService = (ServiceController<DefaultResourceAdapterService>) serviceRegistry.getService(DefaultResourceAdapterService.DEFAULT_RA_NAME_SERVICE_NAME);
            // if a default RA name service is already installed then just update the resource adapter name
            if (existingDefaultRANameService != null) {
                existingDefaultRANameService.getValue().setResourceAdapterName(this.resourceAdapterName);
            } else {
                // create a new one and install
                final DefaultResourceAdapterService defaultResourceAdapterService = new DefaultResourceAdapterService(this.resourceAdapterName);
                context.getServiceTarget().addService(DefaultResourceAdapterService.DEFAULT_RA_NAME_SERVICE_NAME, defaultResourceAdapterService)
                        .install();
            }

            // complete the step
            context.completeStep();
        }
    }
}
