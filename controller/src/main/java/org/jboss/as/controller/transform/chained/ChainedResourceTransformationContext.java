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

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformersLogger;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * Similar to {@link ResourceTransformationContext} but for use with {@link ChainedOperationTransformer} and {@link ChainedOperationTransformerEntry}
 *
 * @deprecated Use {@link TransformationDescriptionBuilder} instead
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Deprecated
public class ChainedResourceTransformationContext implements TransformationContext {
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

    /**
     * Similar to {@link ResourceTransformationContext#resolveTransformer(PathAddress)}
     */
    public ResourceTransformer resolveTransformer(PathAddress address) {
        return delegate.resolveTransformer(address);
    }

    /**
     * Similar to {@link ResourceTransformationContext#getTransformedRoot()}
     */
    public Resource getTransformedRoot() {
        return delegate.getTransformedRoot();
    }

    @Override
    public TransformersLogger getLogger() {
        return delegate.getLogger();
    }

    @Override
    public boolean doesTargetSupportIgnoredResources(TransformationTarget target) {
        return delegate.doesTargetSupportIgnoredResources(target);
    }
}