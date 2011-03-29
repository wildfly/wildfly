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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersService.ModifiableResourceAdapeters;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;

/**
 * Operation handler responsible for adding a Ra.
 * @author maeste
 */
public class RaAdd extends AbstractRaOperation implements ModelAddOperationHandler {
    static final RaAdd INSTANCE = new RaAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        for (final String attribute : ResourceAdaptersSubsystemProviders.RESOURCEADAPTER_ATTRIBUTE) {
            if (operation.get(attribute).isDefined()) {
                model.get(attribute).set(operation.get(attribute));
            }
        }
    }

    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler)
            throws OperationFailedException {
        final ModelNode subModel = context.getSubModel();

        populateModel(operation, subModel);

        // Compensating is remove
        final ModelNode address = operation.require(OP_ADDR);
        final String archive = PathAddress.pathAddress(address).getLastElement().getValue();
        operation.get(ARCHIVE).set(archive);
        final ModelNode compensating = Util.getResourceRemoveOperation(address);

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {

                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceTarget serviceTarget = context.getServiceTarget();

                    ModifiableResourceAdapeters resourceAdapters = buildResourceAdaptersObject(operation);

                    final ServiceController<?> raService = context.getServiceRegistry().getService(
                            ConnectorServices.RESOURCEADAPTERS_SERVICE);
                    if (raService == null) {
                        serviceTarget
                                .addService(ConnectorServices.RESOURCEADAPTERS_SERVICE,
                                        new ResourceAdaptersService(resourceAdapters)).setInitialMode(Mode.ACTIVE).install();
                    } else {
                        ((ModifiableResourceAdapeters) raService.getValue()).addAllResourceAdapters(resourceAdapters
                                .getResourceAdapters());
                    }

                    resultHandler.handleResultComplete();

                }

            });
        } else {
            resultHandler.handleResultComplete();
        }

        return new BasicOperationResult(compensating);

    }

}
