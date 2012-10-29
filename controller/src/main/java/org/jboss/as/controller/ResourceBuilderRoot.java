/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2012, Red Hat Middleware LLC, and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.controller;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
class ResourceBuilderRoot implements ResourceBuilder {
    private final PathElement pathElement;
    private final StandardResourceDescriptionResolver resourceResolver;
    private final List<AttributeBinding> attributes = new LinkedList<AttributeBinding>();
    private final List<OperationBinding> operations = new LinkedList<OperationBinding>();
    private ResourceDescriptionResolver attributeResolver = null;
    private OperationStepHandler addHandler;
    private OperationStepHandler removeHandler;
    private final ResourceBuilderRoot parent;
    private final List<ResourceBuilderRoot> children = new LinkedList<ResourceBuilderRoot>();


    private ResourceBuilderRoot(PathElement pathElement, StandardResourceDescriptionResolver resourceResolver,
                                OperationStepHandler addHandler,
                                OperationStepHandler removeHandler,
                                ResourceBuilderRoot parent) {
        this.pathElement = pathElement;
        this.resourceResolver = resourceResolver;
        this.parent = parent;
        this.addHandler = addHandler;
        this.removeHandler = removeHandler;
    }

    static ResourceBuilder create(PathElement pathElement, StandardResourceDescriptionResolver resourceDescriptionResolver) {
        return new ResourceBuilderRoot(pathElement, resourceDescriptionResolver, null, null, null);
    }

    static ResourceBuilder create(PathElement pathElement, StandardResourceDescriptionResolver resourceResolver,
                                  OperationStepHandler addHandler,
                                  OperationStepHandler removeHandler) {
        return new ResourceBuilderRoot(pathElement, resourceResolver, addHandler, removeHandler, null);
    }


    @Override
    public ResourceBuilder setAddOperation(final AbstractAddStepHandler handler) {
        this.addHandler = handler;
        return this;
    }

    @Override
    public ResourceBuilder setRemoveOperation(final AbstractRemoveStepHandler handler) {
        this.removeHandler = handler;
        return this;
    }

    @Override
    public ResourceBuilder addReadWriteAttribute(AttributeDefinition attributeDefinition, OperationStepHandler reader, OperationStepHandler writer) {
        attributes.add(new AttributeBinding(attributeDefinition, reader, writer, AttributeAccess.AccessType.READ_WRITE));
        return this;
    }

    @Override
    public ResourceBuilder addReadWriteAttributes(OperationStepHandler reader, OperationStepHandler writer, final AttributeDefinition... attributes) {
        for (AttributeDefinition ad : attributes) {
            this.attributes.add(new AttributeBinding(ad, reader, writer, AttributeAccess.AccessType.READ_WRITE));
        }
        return this;
    }


    @Override
    public ResourceBuilder addReadOnlyAttribute(AttributeDefinition attributeDefinition) {
        attributes.add(new AttributeBinding(attributeDefinition, null, null, AttributeAccess.AccessType.READ_ONLY));
        return this;
    }

    @Override
    public ResourceBuilder addMetric(AttributeDefinition attributeDefinition, OperationStepHandler handler) {
        attributes.add(new AttributeBinding(attributeDefinition, handler, null, AttributeAccess.AccessType.METRIC));
        return this;
    }

    @Override
    public ResourceBuilder addMetrics(OperationStepHandler metricHandler, final AttributeDefinition... attributes) {
        for (AttributeDefinition ad : attributes) {
            this.attributes.add(new AttributeBinding(ad, metricHandler, null, AttributeAccess.AccessType.METRIC));
        }
        return this;
    }

    @Override
    public ResourceBuilder setAttributeResolver(ResourceDescriptionResolver resolver) {
        this.attributeResolver = resolver;
        return this;
    }

    @Override
    public ResourceBuilder addOperation(final OperationDefinition operationDefinition, final OperationStepHandler handler) {

        return addOperation(operationDefinition, handler, false);
    }

    @Override
    public ResourceBuilder addOperation(final OperationDefinition operationDefinition, final OperationStepHandler handler, boolean inherited) {
        operations.add(new OperationBinding(operationDefinition, handler, inherited));
        return this;
    }

    public ResourceBuilder pushChild(final PathElement pathElement) {
        return pushChild(pathElement, resourceResolver.getChildResolver(pathElement.getKey()));
    }

    @Override
    public ResourceBuilder pushChild(final PathElement pathElement, final OperationStepHandler addHandler, final OperationStepHandler removeHandler) {
        return pushChild(pathElement, resourceResolver.getChildResolver(pathElement.getKey()), addHandler, removeHandler);
    }

    public ResourceBuilder pushChild(final PathElement pathElement, StandardResourceDescriptionResolver resolver) {
        return pushChild(pathElement, resolver, null, null);
    }

    @Override
    public ResourceBuilder pushChild(final PathElement pathElement, StandardResourceDescriptionResolver resolver, final OperationStepHandler addHandler, final OperationStepHandler removeHandler) {
        ResourceBuilderRoot child = new ResourceBuilderRoot(pathElement, resolver, addHandler, removeHandler, this);
        children.add(child);
        return child;
    }

    @Override
    public ResourceBuilder pushChild(final ResourceBuilder child) {
        ResourceBuilderRoot childDelegate = new ChildBuilderDelegate((ResourceBuilderRoot) child, this);
        children.add(childDelegate);
        return childDelegate;
    }

    @Override
    public ResourceBuilder pop() {
        if (parent == null) {
            return this;
        }
        return parent;
    }


    @Override
    public ResourceDefinition build() {
        if (parent != null) {
            return parent.build();
        }
        return new BuilderResourceDefinition(this);
    }

    List<AttributeBinding> getAttributes() {
        return attributes;
    }

    List<OperationBinding> getOperations() {
        return operations;
    }

    List<ResourceBuilderRoot> getChildren() {
        return children;
    }

    private class BuilderResourceDefinition extends SimpleResourceDefinition {
        final ResourceBuilderRoot builder;

        private BuilderResourceDefinition(ResourceBuilderRoot builder) {
            super(builder.pathElement, builder.resourceResolver, builder.addHandler, builder.removeHandler);
            this.builder = builder;
        }

        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            super.registerOperations(resourceRegistration);
            for (OperationBinding ob : operations) {
                ob.register(resourceRegistration);
            }
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            super.registerAttributes(resourceRegistration);
            for (AttributeBinding ab : builder.attributes) {
                ab.register(resourceRegistration);
            }
        }

        @Override
        public void registerChildren(ManagementResourceRegistration resourceRegistration) {
            super.registerChildren(resourceRegistration);
            for (ResourceBuilderRoot child : builder.children) {
                resourceRegistration.registerSubModel(new BuilderResourceDefinition(child));
            }
        }
    }

    static final class ChildBuilderDelegate extends ResourceBuilderRoot {
        ChildBuilderDelegate(final ResourceBuilderRoot child, final ResourceBuilderRoot parent) {
            super(child.pathElement, child.resourceResolver, child.addHandler, child.removeHandler, parent);
            getChildren().addAll(child.children);
            getAttributes().addAll(child.attributes);
            getOperations().addAll(child.operations);
        }
    }
}

final class AttributeBinding {
    private final AttributeDefinition attribute;
    private final OperationStepHandler readOp;
    private final OperationStepHandler writeOp;
    private final AttributeAccess.AccessType accessType;

    AttributeBinding(AttributeDefinition attribute, OperationStepHandler readOp, OperationStepHandler writeOp, AttributeAccess.AccessType accessType) {
        this.attribute = attribute;
        this.readOp = readOp;
        this.writeOp = writeOp;
        this.accessType = accessType;
    }

    void register(ManagementResourceRegistration registration) {
        if (accessType == AttributeAccess.AccessType.READ_ONLY) {
            registration.registerReadOnlyAttribute(attribute, readOp);
        } else if (accessType == AttributeAccess.AccessType.READ_WRITE) {
            registration.registerReadWriteAttribute(attribute, readOp, writeOp);
        } else if (accessType == AttributeAccess.AccessType.METRIC) {
            registration.registerMetric(attribute, readOp);
        }
    }

}

final class OperationBinding {
    private OperationDefinition definition;
    private OperationStepHandler handler;
    private boolean inherited;

    OperationBinding(OperationDefinition definition, OperationStepHandler handler, boolean inherited) {
        this.definition = definition;
        this.handler = handler;
        this.inherited = inherited;
    }

    public void register(ManagementResourceRegistration registration) {
        registration.registerOperationHandler(definition, handler, inherited);
    }
}


