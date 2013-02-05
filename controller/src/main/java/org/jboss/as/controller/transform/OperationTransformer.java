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

package org.jboss.as.controller.transform;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * The operation transformer. For basic implementations {@see AbstractOperationTransformer}.
 *
* @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
*/
public interface OperationTransformer {

    /**
     * Transform the operation.
     *
     * @param context the operation context
     * @param address the path address
     * @param operation the operation
     * @return the transformed operation
     * @throws OperationFailedException
     */
    TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException;

    public class TransformedOperation implements OperationResultTransformer, OperationRejectionPolicy {

        private final ModelNode transformedOperation;
        private final OperationRejectionPolicy rejectPolicy;
        private final OperationResultTransformer resultTransformer;

        public TransformedOperation(ModelNode transformedOperation, OperationResultTransformer resultTransformer) {
            this(transformedOperation, DEFAULT_REJECTION_POLICY, resultTransformer);
        }

        public TransformedOperation(ModelNode transformedOperation, OperationRejectionPolicy policy, OperationResultTransformer resultTransformer) {
            this.transformedOperation = transformedOperation;
            this.rejectPolicy = policy;
            this.resultTransformer = resultTransformer;
        }

        public ModelNode getTransformedOperation() {
            return transformedOperation;
        }

        public OperationResultTransformer getResultTransformer() {
            return resultTransformer;
        }

        @Override
        public boolean rejectOperation(final ModelNode preparedResult) {
            return rejectPolicy.rejectOperation(preparedResult);
        }

        @Override
        public String getFailureDescription() {
            return rejectPolicy.getFailureDescription();
        }

        @Override
        public ModelNode transformResult(final ModelNode result) {
            return resultTransformer.transformResult(result);
        }

    }

    OperationTransformer DEFAULT = new OperationTransformer() {
       @Override
       public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode original) throws OperationFailedException {
           return new TransformedOperation(original, OperationResultTransformer.ORIGINAL_RESULT);
       }
    };


    OperationTransformer DISCARD = new OperationTransformer() {

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
            return new TransformedOperation(null, DEFAULT_REJECTION_POLICY, SUCCESSFUL_RESULT);
        }
    };

    OperationResultTransformer SUCCESSFUL_RESULT = new OperationResultTransformer() {
        @Override
        public ModelNode transformResult(ModelNode ignored) {
            final ModelNode result = new ModelNode();
            result.get(ModelDescriptionConstants.OUTCOME).set(ModelDescriptionConstants.SUCCESS);
            result.get(ModelDescriptionConstants.RESULT);
            return result;
        }
    };

    OperationRejectionPolicy DEFAULT_REJECTION_POLICY = new OperationRejectionPolicy() {
        @Override
        public boolean rejectOperation(ModelNode preparedResult) {
            return false;
        }

        @Override
        public String getFailureDescription() {
            return null;
        }
    };

}
