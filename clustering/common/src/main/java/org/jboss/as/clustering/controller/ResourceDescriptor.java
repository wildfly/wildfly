/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;

/**
 * Describes the properties of resource used by {@link AddStepHandler}.
 * Supports supplying attributes and capabilities via enumerations.
 * Also supports defining extra parameters that are not actually attributes of the target resource.
 * @author Paul Ferraro
 */
public class ResourceDescriptor implements AddStepHandlerDescriptor {

    private static final Comparator<PathElement> PATH_COMPARATOR = (PathElement path1, PathElement path2) -> {
        int result = path1.getKey().compareTo(path2.getKey());
        return (result == 0) ? path1.getValue().compareTo(path2.getValue()) : result;
    };

    private final ResourceDescriptionResolver resolver;
    private final List<Capability> capabilities = new LinkedList<>();
    private final List<AttributeDefinition> attributes = new LinkedList<>();
    private final List<AttributeDefinition> parameters = new LinkedList<>();
    private final Set<PathElement> requiredChildren = new TreeSet<>(PATH_COMPARATOR);
    private final Set<PathElement> requiredSingletonChildren = new TreeSet<>(PATH_COMPARATOR);

    public ResourceDescriptor(ResourceDescriptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public ResourceDescriptionResolver getDescriptionResolver() {
        return this.resolver;
    }

    @Override
    public Collection<Capability> getCapabilities() {
        return this.capabilities;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return this.attributes;
    }

    @Override
    public Collection<AttributeDefinition> getExtraParameters() {
        return this.parameters;
    }

    @Override
    public Set<PathElement> getRequiredChildren() {
        return this.requiredChildren;
    }

    @Override
    public Set<PathElement> getRequiredSingletonChildren() {
        return this.requiredSingletonChildren;
    }

    public <E extends Enum<E> & Attribute> ResourceDescriptor addAttributes(Class<E> enumClass) {
        return this.addAttributes(EnumSet.allOf(enumClass));
    }

    public ResourceDescriptor addAttributes(Attribute... attributes) {
        return this.addAttributes(Arrays.asList(attributes));
    }

    public ResourceDescriptor addAttributes(Collection<? extends Attribute> attributes) {
        attributes.forEach(attribute -> this.attributes.add(attribute.getDefinition()));
        return this;
    }

    public <E extends Enum<E> & Attribute> ResourceDescriptor addExtraParameters(Class<E> enumClass) {
        return this.addExtraParameters(EnumSet.allOf(enumClass));
    }

    public ResourceDescriptor addExtraParameters(Attribute... parameters) {
        return this.addExtraParameters(Arrays.asList(parameters));
    }

    public ResourceDescriptor addExtraParameters(Collection<? extends Attribute> parameters) {
        parameters.forEach(attribute -> this.parameters.add(attribute.getDefinition()));
        return this;
    }

    public ResourceDescriptor addExtraParameters(AttributeDefinition... parameters) {
        this.parameters.addAll(Arrays.asList(parameters));
        return this;
    }

    public <E extends Enum<E> & Capability> ResourceDescriptor addCapabilities(Class<E> enumClass) {
        return this.addCapabilities(EnumSet.allOf(enumClass));
    }

    public ResourceDescriptor addCapabilities(Capability... capabilities) {
        return this.addCapabilities(Arrays.asList(capabilities));
    }

    public ResourceDescriptor addCapabilities(Collection<? extends Capability> capabilities) {
        this.capabilities.addAll(capabilities);
        return this;
    }

    public <E extends Enum<E> & ResourceDefinition> ResourceDescriptor addRequiredChildren(Class<E> enumClass) {
        EnumSet.allOf(enumClass).forEach(definition -> this.requiredChildren.add(definition.getPathElement()));
        return this;
    }

    public ResourceDescriptor addRequiredChildren(PathElement... paths) {
        this.requiredChildren.addAll(Arrays.asList(paths));
        return this;
    }

    public <E extends Enum<E> & ResourceDefinition> ResourceDescriptor addRequiredSingletonChildren(Class<E> enumClass) {
        EnumSet.allOf(enumClass).forEach(definition -> this.requiredSingletonChildren.add(definition.getPathElement()));
        return this;
    }

    public ResourceDescriptor addRequiredSingletonChildren(PathElement... paths) {
        this.requiredSingletonChildren.addAll(Arrays.asList(paths));
        return this;
    }
}