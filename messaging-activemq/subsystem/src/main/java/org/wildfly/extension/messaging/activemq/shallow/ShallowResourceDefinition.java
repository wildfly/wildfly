/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.shallow;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.CommonAttributes;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public abstract class ShallowResourceDefinition extends PersistentResourceDefinition implements OperationAddressConverter, IgnoredAttributeProvider {

    protected final boolean registerRuntimeOnly;

    public ShallowResourceDefinition(SimpleResourceDefinition.Parameters parameters, boolean registerRuntimeOnly) {
        super(parameters);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        TranslatedOperationHandler handler = new TranslatedOperationHandler(this);
        // Override global operations with transformed operations, if necessary
        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.MapOperations.MAP_CLEAR_DEFINITION, handler);
        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.MapOperations.MAP_PUT_DEFINITION, handler);
        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.MapOperations.MAP_GET_DEFINITION, handler);
        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.MapOperations.MAP_REMOVE_DEFINITION, handler);

        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.ListOperations.LIST_ADD_DEFINITION, handler);
        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.ListOperations.LIST_GET_DEFINITION, handler);
        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.ListOperations.LIST_REMOVE_DEFINITION, handler);
        resourceRegistration.registerOperationHandler(org.jboss.as.controller.operations.global.ListOperations.LIST_CLEAR_DEFINITION, handler);
        super.registerOperations(resourceRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : getAttributes()) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, new TranslatedReadAttributeHandler(this, this), new TranslatedWriteAttributeHandler(this));
            } else {
                registry.registerReadOnlyAttribute(attr, new TranslatedReadAttributeHandler(this, this));
            }
        }
    }

    /**
     * This provides more informative message when a user tries to undefine attribute that is required by the
     * jgroups-discovery-group or socket-discovery-group resources (while it hasn't been required on the original
     * discovery-group resource).
     * @param context
     * @param targetAddress
     * @param translatedOperation
     * @throws org.jboss.as.controller.OperationFailedException
     */
    protected void validateOperation(OperationContext context, PathAddress targetAddress, ModelNode translatedOperation)
            throws OperationFailedException {
        String attributeName = translatedOperation.get(ModelDescriptionConstants.NAME).asString();
        String operationName = translatedOperation.get(ModelDescriptionConstants.OP).asString();
        boolean isUsingSocketBinding = isUsingSocketBinding(targetAddress);

        // if undefining an attribute
        if ((ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION.equals(operationName)
                || ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION.equals(operationName))
                && !translatedOperation.hasDefined(ModelDescriptionConstants.VALUE)) {
            // and the attribute is socket-binding on a socket-discovery-group,
            // or jgroups-cluster on a jgroups-discovery-group, throw an error
            if ((isUsingSocketBinding && CommonAttributes.SOCKET_BINDING.getName().equals(attributeName))
                    || (!isUsingSocketBinding && CommonAttributes.JGROUPS_CLUSTER.getName().equals(attributeName))) {
                throw ControllerLogger.ROOT_LOGGER.validationFailedRequiredParameterNotPresent(attributeName, translatedOperation.toString());
            }
        }
    }

    protected abstract boolean isUsingSocketBinding(PathAddress targetAddress);
}
