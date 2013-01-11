/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.transform.OperationResultTransformer.ORIGINAL_RESULT;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

/**
 * OperationTransformers for legacy versions.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a>
 */
public interface OperationTransformers {

    public static final class MultipleOperationalTransformer implements OperationTransformer {

        private final OperationTransformer[] transformers;

        public MultipleOperationalTransformer(OperationTransformer... transformers) {
            this.transformers = transformers;
        }

        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
            TransformedOperation transformedOperation = new TransformedOperation(operation, ORIGINAL_RESULT);
            for (OperationTransformer transformer : transformers) {
                transformedOperation = transformer.transformOperation(context,address, transformedOperation.getTransformedOperation());
            }
            return transformedOperation;
        }
    }
    /**
     * Transform the operation by removing the given attributes.
     */
    public static final class RemoveAttributesOperationTransformer implements OperationTransformer {

        private final AttributeDefinition[] definitions;

        public RemoveAttributesOperationTransformer(AttributeDefinition... definitions) {
            this.definitions = definitions;

        }
        @Override
        public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation)
                throws OperationFailedException {
            final ModelNode transformedOperation = operation.clone();
            for (AttributeDefinition attr : definitions) {
                transformedOperation.remove(attr.getName());
            }
            return new TransformedOperation(transformedOperation, ORIGINAL_RESULT);
        }
    }

    /**
     * Transform the operation in a failure if the original outcome was not ignored and the operation includes one of the given attributes.
     */
    public static final class FailUnignoredAttributesOperationTransformer implements OperationTransformer {

        private final AttributeDefinition[] definitions;

        public FailUnignoredAttributesOperationTransformer(final AttributeDefinition... definitions) {
            this.definitions = definitions;
        }

        @Override
        public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation)
                throws OperationFailedException {

            final List<String> found = new ArrayList<String>();
            for (AttributeDefinition attr : definitions) {
                if (operation.require(NAME).asString().equals(attr.getName())) {
                    found.add(attr.getName());
                }
            }

            return new TransformedOperation(operation, new OperationRejectionPolicy() {
                @Override
                public boolean rejectOperation(ModelNode preparedResult) {
                    return found.size() > 0;
                }

                @Override
                public String getFailureDescription() {
                    return MESSAGES.unsupportedAttributeInVersion(found.toString(), MessagingExtension.VERSION_1_1_0);
                }
            }, ORIGINAL_RESULT);
        }
    }

    /**
     * Transform the operation by inserting the default values for the given attributes.
     */
    public static final class InsertDefaultValuesOperationTransformer implements OperationTransformer {

        private final AttributeDefinition[] definitions;

        public InsertDefaultValuesOperationTransformer(final AttributeDefinition... definitions) {
            this.definitions = definitions;
        }

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
                throws OperationFailedException {
            for (AttributeDefinition attr : definitions) {
                if (!operation.hasDefined(attr.getName())) {
                    operation.get(attr.getName()).set(attr.getDefaultValue());
                }
            }
            return new TransformedOperation(operation, ORIGINAL_RESULT);
        }
    }

}
