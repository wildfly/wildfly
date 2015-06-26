/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jacorb;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Operation to migrate from the legacy JacORB subsystem to new IIOP-OpenJDK subsystem.
 *
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */

public class MigrateOperation implements OperationStepHandler {

    private static final OperationStepHandler INSTANCE = new MigrateOperation();

    static void registerOperation(final ManagementResourceRegistration registry, final ResourceDescriptionResolver resourceDescriptionResolver) {
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder(JacORBSubsystemConstants.MIGRATE, resourceDescriptionResolver)
                        .setRuntimeOnly()
                        .build(),
                MigrateOperation.INSTANCE);

    }

    private static final PathElement OPENJDK_EXTENSION_ELEMENT = PathElement.pathElement(EXTENSION, "org.wildfly.iiop-openjdk");
    private static final PathElement OPENJDK_SUBSYSTEM_ELEMENT = PathElement.pathElement(SUBSYSTEM, "iiop-openjdk");
    private static final PathElement JACORB_SUBSYSTEM_ELEMENT = PathElement.pathElement(SUBSYSTEM, "jacorb");

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        if (context.getRunningMode() != RunningMode.ADMIN_ONLY) {
            throw new OperationFailedException("the iiop migration can be performed when the server is in admin-only mode");
        }

        final PathAddress subsystemsAddress=context.getCurrentAddress().getParent();

        if (context.readResourceFromRoot(subsystemsAddress).hasChild(OPENJDK_SUBSYSTEM_ELEMENT)) {
            throw new OperationFailedException("can not migrate: the new iiop-openjdk subsystem is already defined");
        }

        if(!context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS).hasChild(OPENJDK_EXTENSION_ELEMENT)) {
            addOpenjdkExtension(context);
        }

        final Resource jacorbResource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode jacorbModel = Resource.Tools.readModel(jacorbResource).clone();

        TransformUtils.checkLegacyModel(jacorbModel, true);

        final ModelNode openjdkModel = TransformUtils.transformModel(jacorbModel);

        final PathAddress openjdkAddress=subsystemsAddress.append(OPENJDK_SUBSYSTEM_ELEMENT);
        addOpenjdkSubsystem(context, openjdkAddress, openjdkModel);

        final PathAddress jacorbAddress=subsystemsAddress.append(JACORB_SUBSYSTEM_ELEMENT);
        removeJacorbSubsystem(context, jacorbAddress);
    }

    private void addOpenjdkExtension(final OperationContext context){
        final OperationStepHandler addExtensionHandler = context.getRootResourceRegistration().getOperationHandler(
                PathAddress.pathAddress(PathElement.pathElement(EXTENSION)), ADD);
        final PathAddress openjdkExtensionAddress = PathAddress.EMPTY_ADDRESS.append(OPENJDK_EXTENSION_ELEMENT);
        final ModelNode openjdkExtensionAddOperation = Util.createAddOperation(openjdkExtensionAddress);
        context.addStep(openjdkExtensionAddOperation, addExtensionHandler, OperationContext.Stage.MODEL);
    }

    private void addOpenjdkSubsystem(final OperationContext context, final PathAddress address, final ModelNode model){
        final ModelNode operation = Util.createAddOperation(address);
        for (final Property property : model.asPropertyList()) {
            if (property.getValue().isDefined()) {
                operation.get(property.getName()).set(property.getValue());
            }
        }

        //operation has to be performed after extension is added so that the handler is available
        context.addStep(operation, new OperationStepHandler() {
            @Override
            public void execute(OperationContext operationContext, ModelNode modelNode) throws OperationFailedException {
                OperationStepHandler addHandler = operationContext.getRootResourceRegistration().getOperationHandler(
                        address, ADD);
                operationContext.addStep(operation, addHandler, OperationContext.Stage.MODEL);
            }
        }, OperationContext.Stage.MODEL);
    }

    private void removeJacorbSubsystem(final OperationContext context, final PathAddress address){
        ModelNode removeLegacySubsystemOperation = Util.createRemoveOperation(address);
        context.addStep(removeLegacySubsystemOperation,
                context.getRootResourceRegistration().getOperationHandler(address, REMOVE),
                OperationContext.Stage.MODEL);
    }
}

