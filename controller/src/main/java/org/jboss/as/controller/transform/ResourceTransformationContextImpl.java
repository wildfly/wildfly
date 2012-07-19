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

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import java.util.Iterator;
import java.util.Set;

/**
* @author Emanuel Muckenhuber
*/
class ResourceTransformationContextImpl implements ResourceTransformationContext {

    private final Resource root;
    private final PathAddress current;
    private final OriginalModel originalModel;

    static ResourceTransformationContext create(final OperationContext context, final TransformationTarget target) {
        final Resource root = Resource.Factory.create();
        final Resource original = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, true);
        final ImmutableManagementResourceRegistration registration = context.getRootResourceRegistration().getSubModel(PathAddress.EMPTY_ADDRESS);
        final ExpressionResolver expressionResolver = TransformerExpressionResolver.create(context, target.getTargetType());
        final OriginalModel originalModel = new OriginalModel(original, context.getRunningMode(), context.getProcessType(), target, registration, expressionResolver);
        return new ResourceTransformationContextImpl(root, PathAddress.EMPTY_ADDRESS, originalModel);
    }

    static ResourceTransformationContext create(TransformationTarget target, Resource model, ImmutableManagementResourceRegistration registration, ExpressionResolver resolver, RunningMode runningMode, ProcessType type) {
        final Resource root = Resource.Factory.create();
        final OriginalModel originalModel = new OriginalModel(model,  runningMode, type, target, registration,resolver);
        return new ResourceTransformationContextImpl(root, PathAddress.EMPTY_ADDRESS, originalModel);
    }

    ResourceTransformationContextImpl(Resource root, PathAddress address, final OriginalModel originalModel) {
        this.root = root;
        this.current = address;
        this.originalModel = originalModel;
    }

    public Resource createResource(final PathAddress element) {
        final PathAddress absoluteAddress = this.current.append(element);
        final Resource resource = Resource.Factory.create();
        addTransformedRecursiveResourceFromRoot(absoluteAddress, resource);
        return resource;
    }

    public Resource createResource(final PathAddress element, Resource copy) {
        final PathAddress absoluteAddress = this.current.append(element);
        final Resource resource = Resource.Factory.create();
        resource.writeModel(copy.getModel());
        addTransformedRecursiveResourceFromRoot(absoluteAddress, resource);
        return resource;
    }

    @Override
    public ResourceTransformationContext addTransformedResource(PathAddress address, Resource toAdd) {
        final PathAddress absoluteAddress = this.current.append(address);
        return addTransformedResourceFromRoot(absoluteAddress, toAdd);
    }

    @Override
    public ResourceTransformationContext addTransformedResourceFromRoot(PathAddress absoluteAddress, Resource toAdd) {
        // Only keep the mode, drop all children
        final Resource copy = Resource.Factory.create();
        if(toAdd != null) {
            copy.writeModel(toAdd.getModel());
        }
        return addTransformedRecursiveResourceFromRoot(absoluteAddress, copy);
    }

    @Override
    public void addTransformedRecursiveResource(PathAddress relativeAddress, Resource resource) {
        final PathAddress absoluteAddress = this.current.append(relativeAddress);
        addTransformedRecursiveResourceFromRoot(absoluteAddress, resource);
    }

    public ResourceTransformationContext addTransformedRecursiveResourceFromRoot(final PathAddress absoluteAddress, final Resource toAdd) {
        Resource model = this.root;
        final Iterator<PathElement> i = absoluteAddress.iterator();
        while (i.hasNext()) {
            final PathElement element = i.next();
            if (element.isMultiTarget()) {
                throw MESSAGES.cannotWriteTo("*");
            }
            if (! i.hasNext()) {
                if(model.hasChild(element)) {
                    throw MESSAGES.duplicateResourceAddress(absoluteAddress);
                } else {
                    model.registerChild(element, toAdd);
                    model = toAdd;
                }
            } else {
                model = model.getChild(element);
                if (model == null) {
                    PathAddress ancestor = PathAddress.EMPTY_ADDRESS;
                    for (PathElement pe : absoluteAddress) {
                        ancestor = ancestor.append(pe);
                        if (element.equals(pe)) {
                            break;
                        }
                    }
                    throw MESSAGES.resourceNotFound(ancestor, absoluteAddress);
                }
            }
        }
        return new ResourceTransformationContextImpl(root, absoluteAddress, originalModel);
    }

    @Override
    public ResourceTransformer resolveTransformer(PathAddress address) {
        final ResourceTransformer transformer = originalModel.target.resolveTransformer(address);
        if(transformer == null) {
            final ImmutableManagementResourceRegistration childReg = originalModel.getRegistration(address);
            if(childReg == null) {
                return ResourceTransformer.DISCARD;
            }
            if(childReg.isRemote() || childReg.isRuntimeOnly()) {
                return ResourceTransformer.DISCARD;
            }
            return ResourceTransformer.DEFAULT;
        }
        return transformer;
    }

    @Override
    public void processChildren(final Resource resource) throws OperationFailedException {
        final Set<String> types = resource.getChildTypes();
        for(final String type : types) {
            for(final Resource.ResourceEntry child : resource.getChildren(type)) {
                processChild(child.getPathElement(), child);
            }
        }
    }

    @Override
    public void processChild(final PathElement element, Resource child) throws OperationFailedException {
        final PathAddress childAddress = current.append(element);
        final ResourceTransformer transformer = resolveTransformer(childAddress);
        final ResourceTransformationContext childContext = new ResourceTransformationContextImpl(root, childAddress, originalModel);
        transformer.transformResource(childContext, childAddress, child);
    }

    @Override
    public TransformationTarget getTarget() {
        return originalModel.target;
    }

    @Override
    public ProcessType getProcessType() {
        return originalModel.type;
    }

    @Override
    public RunningMode getRunningMode() {
        return originalModel.mode;
    }

    @Override
    public ImmutableManagementResourceRegistration getResourceRegistration(PathAddress address) {
        final PathAddress a = current.append(address);
        return originalModel.getRegistration(a);
    }

    @Override
    public ImmutableManagementResourceRegistration getResourceRegistrationFromRoot(PathAddress address) {
        return originalModel.getRegistration(address);
    }

    @Override
    public Resource readResource(PathAddress address) {
        final PathAddress a = current.append(address);
        return originalModel.get(a);
    }

    @Override
    public Resource readResourceFromRoot(PathAddress address) {
        return originalModel.get(address);
    }

    @Override
    public ModelNode resolveExpressions(final ModelNode node) throws OperationFailedException {
        return originalModel.expressionResolver.resolveExpressions(node);
    }

    @Override
    public Resource getTransformedRoot() {
        return root;
    }

    static class OriginalModel {

        private final Resource original;
        private final RunningMode mode;
        private final ProcessType type;
        private final TransformationTarget target;
        private final ImmutableManagementResourceRegistration registration;
        private final ExpressionResolver expressionResolver;

        OriginalModel(Resource original, RunningMode mode, ProcessType type, TransformationTarget target, ImmutableManagementResourceRegistration registration, ExpressionResolver expressionResolver) {
            this.original = original;
            this.mode = mode;
            this.type = type;
            this.target = target;
            this.registration = registration;
            this.expressionResolver = expressionResolver;
        }

        Resource get(final PathAddress address) {
            return original.navigate(address);
        }

        ImmutableManagementResourceRegistration getRegistration(PathAddress address) {
            return registration.getSubModel(address);
        }

    }

}
