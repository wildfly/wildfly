/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Operation handler responsible for disabling an existing data-source.
 *
 * @author Stefano Maestri
 */
public class RaActivate implements OperationStepHandler {
    static final RaActivate INSTANCE = new RaActivate();

    public void execute(OperationContext context, ModelNode operation)  throws OperationFailedException {

        final ModelNode address = operation.require(OP_ADDR);
        final String idName = PathAddress.pathAddress(address).getLastElement().getValue();
        ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        final String archiveOrModuleName;
        if (!model.hasDefined(ARCHIVE.getName()) && !model.hasDefined(MODULE.getName())) {
            throw ConnectorLogger.ROOT_LOGGER.archiveOrModuleRequired();
        }
        if (model.get(ARCHIVE.getName()).isDefined()) {
            archiveOrModuleName = model.get(ARCHIVE.getName()).asString();
        } else {
            archiveOrModuleName = model.get(MODULE.getName()).asString();
        }

        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                public void execute(final OperationContext context, ModelNode operation) throws OperationFailedException {

                    ServiceName restartedServiceName = RaOperationUtil.restartIfPresent(context, archiveOrModuleName, idName);

                    if (restartedServiceName == null) {
                        RaOperationUtil.activate(context, idName, archiveOrModuleName);
                    }
                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            try {
                                RaOperationUtil.removeIfActive(context, archiveOrModuleName, idName);
                            } catch (OperationFailedException e) {

                            }

                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

}
