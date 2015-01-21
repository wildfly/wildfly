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

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MODULE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.STATISTICS_ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Operation handler responsible for adding a Ra.
 *
 * @author maeste
 */
public class RaAdd extends AbstractAddStepHandler {
    static final RaAdd INSTANCE = new RaAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attribute : CommonAttributes.RESOURCE_ADAPTER_ATTRIBUTE) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    public void performRuntime(final OperationContext context, ModelNode operation, final ModelNode model) throws OperationFailedException {
        // Compensating is remove
        final ModelNode address = operation.require(OP_ADDR);
        final String name = PathAddress.pathAddress(address).getLastElement().getValue();
        final String archiveOrModuleName;
        if (!model.hasDefined(ARCHIVE.getName()) && ! model.hasDefined(MODULE.getName())) {
            throw ConnectorLogger.ROOT_LOGGER.archiveOrModuleRequired();
        }
        if (model.get(ARCHIVE.getName()).isDefined()) {
            archiveOrModuleName = model.get(ARCHIVE.getName()).asString();
        } else {
            archiveOrModuleName = model.get(MODULE.getName()).asString();
        }


        ModifiableResourceAdapter resourceAdapter = RaOperationUtil.buildResourceAdaptersObject(name, context, operation, archiveOrModuleName);
        List<ServiceController<?>> newControllers = new ArrayList<ServiceController<?>>();
        if (model.get(ARCHIVE.getName()).isDefined()) {
            RaOperationUtil.installRaServices(context, name, resourceAdapter, newControllers);
        } else {
            RaOperationUtil.installRaServicesAndDeployFromModule(context, name, resourceAdapter, archiveOrModuleName, newControllers);
            if (context.isBooting()) {
                context.addStep(new OperationStepHandler() {
                    public void execute(final OperationContext context, ModelNode operation) throws OperationFailedException {

                        //Next lines activate configuration on module deployed rar
                        //in case there is 2 different resource-adapter config using same module deployed rar
                        // a Deployment sercivice could be already present and need a restart to consider also this
                        //newly added configuration
                        ServiceName restartedServiceName = RaOperationUtil.restartIfPresent(context, archiveOrModuleName, name);
                        if (restartedServiceName == null) {
                            RaOperationUtil.activate(context, name, archiveOrModuleName, STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean());
                        }
                        context.completeStep(new OperationContext.RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                                try {
                                    RaOperationUtil.removeIfActive(context, archiveOrModuleName, name);
                                } catch (OperationFailedException e) {

                                }

                            }
                        });
                    }
                }, OperationContext.Stage.RUNTIME);
            }
        }


    }
}
