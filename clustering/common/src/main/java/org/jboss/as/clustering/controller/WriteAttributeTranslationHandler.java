/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * A write-attribute operation handler that translates a value from another attribute
 * @author Paul Ferraro
 */
public class WriteAttributeTranslationHandler implements OperationStepHandler {

    private final AttributeTranslation translation;

    public WriteAttributeTranslationHandler(AttributeTranslation translation) {
        this.translation = translation;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ModelNode value = context.resolveExpressions(operation.get(ModelDescriptionConstants.VALUE));
        ModelNode targetValue = this.translation.getWriteTranslator().translate(context, value);
        Attribute targetAttribute = this.translation.getTargetAttribute();
        PathAddress currentAddress = context.getCurrentAddress();
        PathAddress targetAddress = this.translation.getPathAddressTransformation().apply(currentAddress);
        ModelNode targetOperation = Util.getWriteAttributeOperation(targetAddress, targetAttribute.getName(), targetValue);
        ImmutableManagementResourceRegistration registration = (currentAddress == targetAddress) ? context.getResourceRegistration() : context.getRootResourceRegistration().getSubModel(targetAddress);
        if (registration == null) {
            throw new OperationFailedException(ControllerLogger.MGMT_OP_LOGGER.noSuchResourceType(targetAddress));
        }
        OperationStepHandler writeAttributeHandler = registration.getAttributeAccess(PathAddress.EMPTY_ADDRESS, targetAttribute.getName()).getWriteHandler();
        if (targetAddress == currentAddress) {
            writeAttributeHandler.execute(context, targetOperation);
        } else {
            context.addStep(targetOperation, writeAttributeHandler, context.getCurrentStage());
        }
    }
}
