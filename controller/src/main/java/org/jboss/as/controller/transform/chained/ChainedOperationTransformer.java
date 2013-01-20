/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.transform.chained;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

/**
 * An experimental operation transformer allowing you to chain several transformers.
 *
 * @deprecated Experimental and likely to change
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Deprecated
public class LChainedOperationTransformer implements OperationTransformer {

    private final OperationTransformer[] entries;

    public ChainedOperationTransformer(OperationTransformer...entries) {
        this.entries = entries;
    }

    @Override
    public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
            throws OperationFailedException {
        List<TransformedOperation> delegates = new ArrayList<TransformedOperation>();
        ModelNode currentOperation = operation;
        for (OperationTransformer entry : entries) {
            TransformedOperation transformed = entry.transformOperation(context, address, currentOperation);
            currentOperation = transformed.getTransformedOperation();
            delegates.add(transformed);
            if (currentOperation == null) {
                break;
            }
        }
        return new ChainedTransformedOperation(currentOperation, delegates.toArray(new TransformedOperation[delegates.size()]));
    }

    private static class ChainedTransformedOperation extends TransformedOperation {

        private TransformedOperation[] delegates;
        private volatile String failure;

        public ChainedTransformedOperation(ModelNode transformedOperation, TransformedOperation...delegates) {
            // FIXME ChainedTransformedOperation constructor
            super(transformedOperation, null);
            this.delegates = delegates;
        }

        @Override
        public ModelNode getTransformedOperation() {
            return super.getTransformedOperation();
        }

        @Override
        public OperationResultTransformer getResultTransformer() {
            return this;
        }

        @Override
        public boolean rejectOperation(ModelNode preparedResult) {
            for (TransformedOperation delegate : delegates) {
                if (delegate.rejectOperation(preparedResult)) {
                    failure = delegate.getFailureDescription();
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getFailureDescription() {
            return failure;
        }

        @Override
        public ModelNode transformResult(ModelNode result) {
            ModelNode currentResult = result;
            for (int i = delegates.length - 1 ; i >= 0 ; --i) {
                currentResult = delegates[i].transformResult(currentResult);
            }
            return currentResult;
        }
    }
}
