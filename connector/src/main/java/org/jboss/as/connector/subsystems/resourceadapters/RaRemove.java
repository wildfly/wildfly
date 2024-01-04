/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MODULE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTERS_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.connector._private.Capabilities;
import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
public class RaRemove implements OperationStepHandler {
    static final RaRemove INSTANCE = new RaRemove();

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final ModelNode opAddr = operation.require(OP_ADDR);
        final String idName = PathAddress.pathAddress(opAddr).getLastElement().getValue();
        final boolean isModule;


        // Compensating is add
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS, false).getModel();
        final String archiveOrModuleName;
        if (!model.hasDefined(ARCHIVE.getName()) && !model.hasDefined(MODULE.getName())) {
            throw ConnectorLogger.ROOT_LOGGER.archiveOrModuleRequired();
        }
        if (model.get(ARCHIVE.getName()).isDefined()) {
            isModule = false;
            archiveOrModuleName = model.get(ARCHIVE.getName()).asString();
        } else {
            isModule = true;
            archiveOrModuleName = model.get(MODULE.getName()).asString();
        }
        final ModelNode compensating = Util.getEmptyOperation(ADD, opAddr);

        if (model.hasDefined(RESOURCEADAPTERS_NAME)) {
            for (ModelNode raNode : model.get(RESOURCEADAPTERS_NAME).asList()) {
                ModelNode raCompensatingNode = raNode.clone();
                compensating.get(RESOURCEADAPTERS_NAME).add(raCompensatingNode);
            }
        }


        context.removeResource(PathAddress.EMPTY_ADDRESS);
        context.deregisterCapability(Capabilities.RESOURCE_ADAPTER_CAPABILITY.getDynamicName(context.getCurrentAddress()));

        if (context.isDefaultRequiresRuntime()) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final boolean wasActive;
                    wasActive = RaOperationUtil.removeIfActive(context, archiveOrModuleName, idName);

                    if (wasActive && !context.isResourceServiceRestartAllowed()) {
                        context.reloadRequired();
                        context.completeStep(new OperationContext.RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                                context.revertReloadRequired();
                            }
                        });
                        return;
                    }

                    ServiceName raServiceName = ServiceName.of(ConnectorServices.RA_SERVICE, idName);
                    ServiceController<?> serviceController = context.getServiceRegistry(false).getService(raServiceName);
                    final ModifiableResourceAdapter resourceAdapter;
                    if (serviceController != null) {
                        resourceAdapter = (ModifiableResourceAdapter) serviceController.getValue();
                    } else {
                        resourceAdapter = null;
                    }
                    final List<ServiceName> serviceNameList = context.getServiceRegistry(false).getServiceNames();
                    for (ServiceName name : serviceNameList) {
                        if (raServiceName.isParentOf(name)) {
                            context.removeService(name);
                        }

                    }

                    if (model.get(MODULE.getName()).isDefined()) {
                        //ServiceName deploymentServiceName = ConnectorServices.getDeploymentServiceName(model.get(MODULE.getName()).asString(),raId);
                        //context.removeService(deploymentServiceName);
                        ServiceName deployerServiceName = ConnectorServices.RESOURCE_ADAPTER_DEPLOYER_SERVICE_PREFIX.append(idName);
                        context.removeService(deployerServiceName);
                        ServiceName inactiveServiceName = ConnectorServices.INACTIVE_RESOURCE_ADAPTER_SERVICE.append(idName);
                        context.removeService(inactiveServiceName);
                    }


                    context.removeService(raServiceName);
                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            if (resourceAdapter != null) {
                                List<ServiceController<?>> newControllers = new LinkedList<ServiceController<?>>();
                                if (model.get(ARCHIVE.getName()).isDefined()) {
                                    RaOperationUtil.installRaServices(context, idName, resourceAdapter, newControllers);
                                } else {
                                    try {
                                        RaOperationUtil.installRaServicesAndDeployFromModule(context, idName, resourceAdapter, archiveOrModuleName, newControllers);
                                    } catch (OperationFailedException e) {

                                    }
                                }
                                try {
                                    if (wasActive) {
                                        RaOperationUtil.activate(context, idName, archiveOrModuleName);

                                    }
                                } catch (OperationFailedException e) {

                                }
                            }

                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }
}
