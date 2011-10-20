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

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Operation handler responsible for adding a Ra.
 *
 * @author maeste
 */
public class RaAdd implements OperationStepHandler {
    static final RaAdd INSTANCE = new RaAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        for (final String attribute : ResourceAdaptersSubsystemProviders.RESOURCEADAPTER_ATTRIBUTE) {
            if (operation.get(attribute).isDefined()) {
                model.get(attribute).set(operation.get(attribute));
            }
        }
    }

    public void execute(OperationContext context, ModelNode operation) {
        final ModelNode subModel = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS);
        populateModel(operation, subModel);

        // Compensating is remove
        final ModelNode address = operation.require(OP_ADDR);
        final String archive = PathAddress.pathAddress(address).getLastElement().getValue();
        operation.get(ARCHIVE.getName()).set(archive);

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final ServiceTarget serviceTarget = context.getServiceTarget();
                    final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();

                    ModifiableResourceAdapter resourceAdapter = RaOperationUtil.buildResourceAdaptersObject(operation);

                    final ServiceController<?> resourceAdaptersService = context.getServiceRegistry(false).getService(
                            ConnectorServices.RESOURCEADAPTERS_SERVICE);
                    ServiceController<?> controller = null;
                    if (resourceAdaptersService == null) {
                        controller = serviceTarget.addService(ConnectorServices.RESOURCEADAPTERS_SERVICE,
                                new ResourceAdaptersService()).setInitialMode(Mode.ACTIVE).addListener(verificationHandler).install();
                    }
                    ServiceName raServiceName = ServiceName.of(ConnectorServices.RA_SERVICE, archive);
                    ResourceAdapterService raService = new ResourceAdapterService(resourceAdapter);
                    serviceTarget.addService(raServiceName, raService).setInitialMode(Mode.ACTIVE)
                            .addDependency(ConnectorServices.RESOURCEADAPTERS_SERVICE, ResourceAdaptersService.ModifiableResourceAdaptors.class, raService.getResourceAdaptersInjector())
                            .addListener(verificationHandler).install();

                    context.addStep(verificationHandler, OperationContext.Stage.VERIFY);

                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        if (controller != null) {
                            context.removeService(ConnectorServices.RESOURCEADAPTERS_SERVICE);
                        }
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }
}
