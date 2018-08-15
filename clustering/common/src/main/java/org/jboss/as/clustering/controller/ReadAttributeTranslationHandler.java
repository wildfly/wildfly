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
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * A read-attribute operation handler that translates a value from another attribute
 * @author Paul Ferraro
 */
public class ReadAttributeTranslationHandler implements OperationStepHandler {

    private final AttributeTranslation translation;

    public ReadAttributeTranslationHandler(AttributeTranslation translation) {
        this.translation = translation;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress currentAddress = context.getCurrentAddress();
        PathAddress targetAddress = this.translation.getPathAddressTransformation().apply(currentAddress);
        Attribute targetAttribute = this.translation.getTargetAttribute();
        ModelNode targetOperation = Operations.createReadAttributeOperation(targetAddress, targetAttribute);
        ImmutableManagementResourceRegistration targetRegistration = this.translation.getResourceRegistrationTransformation().apply(context.getResourceRegistration());
        OperationStepHandler readAttributeHandler = targetRegistration.getAttributeAccess(PathAddress.EMPTY_ADDRESS, targetAttribute.getName()).getReadHandler();
        OperationStepHandler readTranslatedAttributeHandler = new ReadTranslatedAttributeStepHandler(readAttributeHandler, targetAttribute, this.translation.getReadTranslator());
        // If targetOperation applies to the current resource, we can execute in the current step
        if (targetAddress == currentAddress) {
            readTranslatedAttributeHandler.execute(context, targetOperation);
        } else {
            context.addStep(targetOperation, readTranslatedAttributeHandler, context.getCurrentStage(), true);
        }
    }

    private static class ReadTranslatedAttributeStepHandler implements OperationStepHandler {
        private final OperationStepHandler readHandler;
        private final Attribute targetAttribute;
        private final AttributeValueTranslator translator;

        ReadTranslatedAttributeStepHandler(OperationStepHandler readHandler, Attribute targetAttribute, AttributeValueTranslator translator) {
            this.readHandler = readHandler;
            this.targetAttribute = targetAttribute;
            this.translator = translator;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (this.readHandler != null) {
                this.readHandler.execute(context, operation);
            } else {
                try {
                    // If attribute has no read handler, simulate one
                    ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS, false).getModel();
                    ModelNode result = context.getResult();
                    if (model.hasDefined(this.targetAttribute.getName())) {
                        result.set(model.get(this.targetAttribute.getName()));
                    } else if (Operations.isIncludeDefaults(operation)) {
                        result.set(this.targetAttribute.getDefinition().getDefaultValue());
                    }
                } catch (Resource.NoSuchResourceException ignore) {
                    // If the target resource does not exist return UNDEFINED
                    return;
                }
            }
            ModelNode result = context.getResult();
            result.set(this.translator.translate(context, result));
        }
    }
}
