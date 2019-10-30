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

package org.jboss.as.messaging;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
interface OperationValidator {

    /**
     * Validate.
     *
     * @param operation the operation to validate
     * @throws org.jboss.as.controller.OperationFailedException
     */
    void validate(ModelNode operation) throws OperationFailedException;

    /**
     * Validate resolved.
     *
     * @param context the operation context
     * @param operation the operation to validate
     * @throws OperationFailedException
     */
    void validateResolved(OperationContext context, ModelNode operation) throws OperationFailedException;

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
        public void validateResolved(final OperationContext context, final ModelNode operation) throws OperationFailedException {
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
                    definition.resolveModelAttribute(context, operation);
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
