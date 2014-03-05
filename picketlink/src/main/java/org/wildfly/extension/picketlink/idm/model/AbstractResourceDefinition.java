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

package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.picketlink.idm.IDMExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 18, 2012
 */
public abstract class AbstractResourceDefinition extends SimpleResourceDefinition {

    private static final Map<ModelElement, List<SimpleAttributeDefinition>> attributeDefinitions;
    private static final Map<ModelElement, List<ResourceDefinition>> childResourceDefinitions;

    static {
        attributeDefinitions = new HashMap<ModelElement, List<SimpleAttributeDefinition>>();
        childResourceDefinitions = new HashMap<ModelElement, List<ResourceDefinition>>();
    }

    private final ModelElement modelElement;
    private final List<SimpleAttributeDefinition> attributes = new ArrayList<SimpleAttributeDefinition>();

    protected AbstractResourceDefinition(ModelElement modelElement, final OperationStepHandler addHandler,
                                            final OperationStepHandler removeHandler, SimpleAttributeDefinition... attributes) {
        super(PathElement.pathElement(modelElement.getName()), IDMExtension.getResourceDescriptionResolver(modelElement.getName()), addHandler, removeHandler);
        this.modelElement = modelElement;
        Collections.addAll(this.attributes, attributes);
    }

    protected AbstractResourceDefinition(ModelElement modelElement, String name, final OperationStepHandler addHandler, SimpleAttributeDefinition... attributes) {
        super(PathElement.pathElement(modelElement.getName(), name), IDMExtension.getResourceDescriptionResolver(modelElement.getName()), addHandler, IDMConfigRemoveStepHandler.INSTANCE);
        this.modelElement = modelElement;
        Collections.addAll(this.attributes, attributes);
    }

    protected AbstractResourceDefinition(ModelElement modelElement, final OperationStepHandler addHandler,
                                            SimpleAttributeDefinition... attributes) {
        super(PathElement.pathElement(modelElement.getName()), IDMExtension.getResourceDescriptionResolver(modelElement.getName()), addHandler, IDMConfigRemoveStepHandler.INSTANCE);
        this.modelElement = modelElement;
        Collections.addAll(this.attributes, attributes);
    }

    public static List<SimpleAttributeDefinition> getAttributeDefinition(ModelElement modelElement) {
        List<SimpleAttributeDefinition> definitions = attributeDefinitions.get(modelElement);

        if (definitions == null) {
            return Collections.emptyList();
        }

        return definitions;
    }

    public static Map<ModelElement, List<ResourceDefinition>> getChildResourceDefinitions() {
        return Collections.unmodifiableMap(childResourceDefinitions);
    }

    protected void addAttribute(SimpleAttributeDefinition attribute) {
        this.attributes.add(attribute);
    }

    private void addAttributeDefinition(ModelElement resourceDefinitionKey, SimpleAttributeDefinition attribute) {
        List<SimpleAttributeDefinition> resourceAttributes = attributeDefinitions.get(resourceDefinitionKey);

        if (resourceAttributes == null) {
            resourceAttributes = new ArrayList<SimpleAttributeDefinition>();
            attributeDefinitions.put(resourceDefinitionKey, resourceAttributes);
        }

        if (!resourceAttributes.contains(attribute)) {
            resourceAttributes.add(attribute);
        }
    }

    private void addChildResourceDefinition(ModelElement resourceDefinitionKey, ResourceDefinition attribute) {
        List<ResourceDefinition> childResources = childResourceDefinitions.get(resourceDefinitionKey);

        if (childResources == null) {
            childResources = new ArrayList<ResourceDefinition>();
            childResourceDefinitions.put(resourceDefinitionKey, childResources);
        }

        if (!childResources.contains(attribute)) {
            childResources.add(attribute);
        }
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (SimpleAttributeDefinition attribute : getAttributes()) {
            addAttributeDefinition(attribute, createAttributeWriterHandler(), resourceRegistration);
        }
    }

    private OperationStepHandler createAttributeWriterHandler() {
        return new IDMConfigWriteAttributeHandler(this.attributes.toArray(new AttributeDefinition[this.attributes.size()]));
    }

    public List<SimpleAttributeDefinition> getAttributes() {
        return Collections.unmodifiableList(this.attributes);
    }

    private void addAttributeDefinition(SimpleAttributeDefinition definition, OperationStepHandler writeHandler, ManagementResourceRegistration resourceRegistration) {
        addAttributeDefinition(this.modelElement, definition);
        resourceRegistration.registerReadWriteAttribute(definition, null, writeHandler);
    }

    protected void addChildResourceDefinition(ResourceDefinition definition, ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(this.modelElement, definition);
        resourceRegistration.registerSubModel(definition);
    }
}
