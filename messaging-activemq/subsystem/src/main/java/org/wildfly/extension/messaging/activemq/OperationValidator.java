/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * @author Emanuel Muckenhuber
 */
interface OperationValidator {

    /**
     * Validate.
     *
     * @param operation the operation to validate
     * @throws OperationFailedException
     */
    void validate(ModelNode operation) throws OperationFailedException;

    /**
     * Validate and Set
     *
     * @param operation the operation to validate
     * @param subModel the subModel
     * @throws OperationFailedException
     */
    void validateAndSet(ModelNode operation, ModelNode subModel) throws OperationFailedException;

    static class AttributeDefinitionOperationValidator implements OperationValidator {

        private final AttributeDefinition[] attributes;
        public AttributeDefinitionOperationValidator(AttributeDefinition... attributes) {
            this.attributes = attributes;
        }

        @Override
        public void validate(final ModelNode operation) throws  OperationFailedException {
            for(final AttributeDefinition definition : attributes) {
                final String attributeName = definition.getName();
                final boolean has = operation.has(attributeName);
                if(! has && definition.isRequired(operation)) {
                    throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.required(definition.getName()));
                }
                if(has) {
                    if(! definition.isAllowed(operation)) {
                        throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.invalid(definition.getName()));
                    }
                    definition.validateOperation(operation);
                }
            }
        }

        @Override
        public void validateAndSet(final ModelNode operation, final ModelNode subModel) throws OperationFailedException {
            for(final AttributeDefinition definition : attributes) {
                final String attributeName = definition.getName();
                final boolean has = operation.has(attributeName);
                if(! has && definition.isRequired(operation)) {
                    throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.required(definition.getName()));
                }
                if(has) {
                    if(! definition.isAllowed(operation)) {
                        throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.invalid(definition.getName()));
                    }
                    definition.validateAndSet(operation, subModel);
                }
            }
        }
    }

}
