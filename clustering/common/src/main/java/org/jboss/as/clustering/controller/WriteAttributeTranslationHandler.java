/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
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
        ModelNode value = context.resolveExpressions(Operations.getAttributeValue(operation));
        ModelNode targetValue = this.translation.getWriteTranslator().translate(context, value);
        Attribute targetAttribute = this.translation.getTargetAttribute();
        PathAddress currentAddress = context.getCurrentAddress();
        PathAddress targetAddress = this.translation.getPathAddressTransformation().apply(currentAddress);
        ModelNode targetOperation = Operations.createWriteAttributeOperation(targetAddress, targetAttribute, targetValue);
        ImmutableManagementResourceRegistration targetRegistration = this.translation.getResourceRegistrationTransformation().apply(context.getResourceRegistration());
        OperationStepHandler writeAttributeHandler = targetRegistration.getAttributeAccess(PathAddress.EMPTY_ADDRESS, targetAttribute.getName()).getWriteHandler();
        if (targetAddress == currentAddress) {
            writeAttributeHandler.execute(context, targetOperation);
        } else {
            context.addStep(targetOperation, writeAttributeHandler, context.getCurrentStage());
        }
    }
}
