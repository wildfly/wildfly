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

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformersLogger;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Emanuel Muckenhuber
 */
abstract class TransformationRule {

    abstract void transformOperation(ModelNode operation, PathAddress address, ChainedOperationContext context) throws OperationFailedException;
    abstract void transformResource(Resource resource, PathAddress address, ChainedResourceContext context) throws OperationFailedException;

    static ModelNode cloneAndProtect(ModelNode modelNode) {
        ModelNode clone = modelNode.clone();
        clone.protect();
        return clone;
    }

    abstract static class AbstractChainedContext {

        private final TransformationContextWrapper context;
        protected AbstractChainedContext(final TransformationContext context) {
            this.context = new TransformationContextWrapper(context);
        }

        protected TransformationContext getContext() {
            return context;
        }

        void setImmutableResource(boolean immutable) {
            context.setImmutableResource(immutable);
        }
    }

    abstract static class ChainedOperationContext extends AbstractChainedContext {

        private final List<OperationTransformer.TransformedOperation> transformed = new ArrayList<OperationTransformer.TransformedOperation>();
        private ModelNode lastOperation;
        protected ChainedOperationContext(TransformationContext context) {
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

    abstract static class ChainedResourceContext extends AbstractChainedContext {

        protected ChainedResourceContext(ResourceTransformationContext context) {
            super(context);
        }

        protected TransformationContext getContext() {
            return super.getContext();
        }

        abstract void invokeNext(Resource resource) throws OperationFailedException;

    }

    private static class TransformationContextWrapper implements TransformationContext {

        private final TransformationContext delegate;
        private volatile boolean immutable;
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
            Resource resource = delegate.readResource(address);
            if (resource != null) {
                return immutable ? new ProtectedModelResource(resource) : resource;
            }
            return null;
        }

        @Override
        public Resource readResourceFromRoot(PathAddress address) {
            Resource resource = delegate.readResourceFromRoot(address);
            if (resource != null) {
                return immutable ? new ProtectedModelResource(resource) : resource;
            }
            return null;
        }

        @Override
        @Deprecated
        public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
            return delegate.resolveExpressions(node);
        }

        @Override
        public TransformersLogger getLogger() {
            return delegate.getLogger();
        }

        @Override
        public boolean doesTargetSupportIgnoredResources(TransformationTarget target) {
            return delegate.doesTargetSupportIgnoredResources(target);
        }

        void setImmutableResource(boolean immutable) {
            this.immutable = immutable;
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

   /**
    *  Implementation of resource that returns an unmodifiable model
    */
   private static class ProtectedModelResource implements Resource {

       private Resource delegate;

       ProtectedModelResource(Resource delegate){
           this.delegate = delegate;
       }

       @Override
       public ModelNode getModel() {
           return TransformationRule.cloneAndProtect(delegate.getModel());
       }

       @Override
       public void writeModel(ModelNode newModel) {
           throw MESSAGES.immutableResource();
       }

       @Override
       public boolean isModelDefined() {
           return delegate.isModelDefined();
       }

       @Override
       public boolean hasChild(PathElement element) {
           return delegate.hasChild(element);
       }

       @Override
       public Resource getChild(PathElement element) {
           Resource resource = delegate.getChild(element);
           if (resource != null) {
               return new ProtectedModelResource(resource);
           }
           return null;
       }

       @Override
       public Resource requireChild(PathElement element) {
           Resource resource = delegate.requireChild(element);
           if (resource != null) {
               return new ProtectedModelResource(resource);
           }
           return null;
       }

       @Override
       public boolean hasChildren(String childType) {
           return delegate.hasChildren(childType);
       }

       @Override
       public Resource navigate(PathAddress address) {
           Resource resource = delegate.navigate(address);
           if (resource != null) {
               return new ProtectedModelResource(resource);
           }
           return null;
       }

       @Override
       public Set<String> getChildTypes() {
           return delegate.getChildTypes();
       }

       @Override
       public Set<String> getChildrenNames(String childType) {
           return delegate.getChildrenNames(childType);
       }

       @Override
       public Set<ResourceEntry> getChildren(String childType) {
           Set<ResourceEntry> children = delegate.getChildren(childType);
           if (children != null) {
               Set<ResourceEntry> protectedChildren = new LinkedHashSet<Resource.ResourceEntry>();
               for (ResourceEntry entry : children) {
                   protectedChildren.add(new ProtectedModelResourceEntry(entry));
               }
           }
           return null;
       }

       @Override
       public void registerChild(PathElement address, Resource resource) {
           throw MESSAGES.immutableResource();
       }

       @Override
       public Resource removeChild(PathElement address) {
           throw MESSAGES.immutableResource();
       }

       @Override
       public boolean isRuntime() {
           return delegate.isRuntime();
       }

       @Override
       public boolean isProxy() {
           return delegate.isProxy();
       }

       public Resource clone() {
           return new ProtectedModelResource(delegate.clone());
       }
   }


    private static class ProtectedModelResourceEntry extends ProtectedModelResource implements ResourceEntry {
        ResourceEntry delegate;

        ProtectedModelResourceEntry(ResourceEntry delegate){
            super(delegate);
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public PathElement getPathElement() {
            return delegate.getPathElement();
        }
    }
}
