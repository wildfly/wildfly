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
package org.jboss.as.controller.transform;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ChainedResourceTransformer implements ResourceTransformer {

    private final ChainedResourceTransformerEntry[] entries;

    public ChainedResourceTransformer(ChainedResourceTransformerEntry...entries) {
        this.entries = entries;
    }

    @Override
    public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource)
            throws OperationFailedException {
        if (resource.isProxy() || resource.isRuntime()) {
            return;
        }

        ChainedResourceTransformationContext wrappedContext = new ChainedResourceTransformationContext(context);
        for (ChainedResourceTransformerEntry entry : entries) {
            entry.transformResource(wrappedContext, address, resource);
        }

        final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
        childContext.processChildren(resource);
    }

    /**
     * Provides chained transformation.
     *
     */
    public interface ChainedResourceTransformerEntry extends ResourceTransformer {

        /**
         * Same as {@link ResourceTransformer#transformResource(ResourceTransformationContext, PathAddress, Resource)} with the exception
         * that you cannot call {@link ResourceTransformationContext#addTransformedResource(PathAddress, Resource)}, {@link ResourceTransformationContext#addTransformedResourceFromRoot(PathAddress, Resource)},
         * {@link ResourceTransformationContext#addTransformedRecursiveResource(PathAddress, Resource)}, {@link ResourceTransformationContext#processChild(PathElement, Resource)} or
         * {@link ResourceTransformationContext#processChildren(Resource)} on the passed in {@code context}.
         */
        void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException;
    }

    private static class ChainedResourceTransformationContext implements ResourceTransformationContext {
        private final ResourceTransformationContext delegate;

        ChainedResourceTransformationContext(ResourceTransformationContext delegate) {
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
        public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
            return delegate.resolveExpressions(node);
        }

        @Override
        public ResourceTransformer resolveTransformer(PathAddress address) {
            return delegate.resolveTransformer(address);
        }


        @Override
        public Resource getTransformedRoot() {
            return delegate.getTransformedRoot();
        }

        @Override
        public ResourceTransformationContext addTransformedResource(PathAddress relativeAddress, Resource resource) {
            throw MESSAGES.cannotCallMethodFromChainedTransformer();
        }

        @Override
        public ResourceTransformationContext addTransformedResourceFromRoot(PathAddress absoluteAddress, Resource resource) {
            throw MESSAGES.cannotCallMethodFromChainedTransformer();
        }

        @Override
        public void addTransformedRecursiveResource(PathAddress relativeAddress, Resource resource) {
            throw MESSAGES.cannotCallMethodFromChainedTransformer();
        }

        @Override
        public void processChildren(Resource resource) throws OperationFailedException {
            throw MESSAGES.cannotCallMethodFromChainedTransformer();
        }

        @Override
        public void processChild(PathElement element, Resource child) throws OperationFailedException {
            throw MESSAGES.cannotCallMethodFromChainedTransformer();
        }
    }
}
