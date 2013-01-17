/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.transform.description;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.dmr.ModelNode;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Emanuel Muckenhuber
 */
abstract class TransformationRule {

    abstract void transformOperation(ModelNode operation, PathAddress address, OperationContext context) throws OperationFailedException;
    abstract void tranformResource(Resource resource, PathAddress address, ResourceContext context) throws OperationFailedException;

    abstract static class AbstractTransformationContext {

        private final TransformationContext context;
        protected AbstractTransformationContext(TransformationContext context) {
            this.context = new TransformationContextWrapper(context);
        }

        protected TransformationContext getContext() {
            return context;
        }

    }

    abstract static class OperationContext extends AbstractTransformationContext {

        private final List<OperationTransformer.TransformedOperation> transformed = new ArrayList<OperationTransformer.TransformedOperation>();
        private ModelNode lastOperation;
        protected OperationContext(TransformationContext context) {
            super(context);
        }

        protected void recordTransformedOperation(OperationTransformer.TransformedOperation operation) {
            lastOperation = operation.getTransformedOperation();
            transformed.add(operation);
        }

        void invokeNext(ModelNode operation) throws OperationFailedException {
            invokeNext(new OperationTransformer.TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT));
        }

        abstract void invokeNext(OperationTransformer.TransformedOperation transformedOperation) throws OperationFailedException;

        protected OperationTransformer.TransformedOperation createOp() {
            if(transformed.size() == 1) {
                return transformed.get(0);
            }
            return new ChainedTransformedOperation(lastOperation, transformed);
        }

    }

    abstract static class ResourceContext extends AbstractTransformationContext {

        protected ResourceContext(ResourceTransformationContext context) {
            super(context);
        }

        protected ResourceTransformationContext getContext() {
            return (ResourceTransformationContext ) super.getContext();
        }

        abstract void invokeNext(Resource resource) throws OperationFailedException;

    }

    private static class TransformationContextWrapper implements TransformationContext {

        private final TransformationContext delegate;
        private TransformationContextWrapper(TransformationContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public TransformationTarget getTarget() {
            return delegate.getTarget();
        }

        @Override
        public ProcessType getProcessType() {
            return delegate.getProcessType();
        }

        @Override
        public RunningMode getRunningMode() {
            return delegate.getRunningMode();
        }

        @Override
        public ImmutableManagementResourceRegistration getResourceRegistration(PathAddress address) {
            return delegate.getResourceRegistration(address);
        }

        @Override
        public ImmutableManagementResourceRegistration getResourceRegistrationFromRoot(PathAddress address) {
            return delegate.getResourceRegistrationFromRoot(address);
        }

        @Override
        public Resource readResource(PathAddress address) {
            return delegate.readResource(address);
        }

        @Override
        public Resource readResourceFromRoot(PathAddress address) {
            return delegate.readResourceFromRoot(address);
        }

        @Override
        @Deprecated
        public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
            return delegate.resolveExpressions(node);
        }
    }

    private static class ChainedTransformedOperation extends OperationTransformer.TransformedOperation {

        private final List<OperationTransformer.TransformedOperation> delegates;
        private volatile String failure;

        public ChainedTransformedOperation(final ModelNode transformedOperation, final List<OperationTransformer.TransformedOperation> delegates) {
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
            for (OperationTransformer.TransformedOperation delegate : delegates) {
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
            final int size = delegates.size();
            for (int i = size - 1 ; i >= 0 ; --i) {
                currentResult = delegates.get(i).transformResult(currentResult);
            }
            return currentResult;
        }
    }


}
