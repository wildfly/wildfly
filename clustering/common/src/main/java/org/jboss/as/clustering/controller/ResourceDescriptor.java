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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.jboss.as.clustering.function.Predicates;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Describes the properties of resource used by {@link AddStepHandler}.
 * Supports supplying attributes and capabilities via enumerations.
 * Also supports defining extra parameters that are not actually attributes of the target resource.
 * @author Paul Ferraro
 */
public class ResourceDescriptor implements AddStepHandlerDescriptor {

    private static final Comparator<PathElement> PATH_COMPARATOR = new Comparator<>() {
        @Override
        public int compare(PathElement path1, PathElement path2) {
            int result = path1.getKey().compareTo(path2.getKey());
            return (result == 0) ? path1.getValue().compareTo(path2.getValue()) : result;
        }
    };
    private static final Comparator<AttributeDefinition> ATTRIBUTE_COMPARATOR = Comparator.comparing(AttributeDefinition::getName);
    private static final Comparator<Capability> CAPABILITY_COMPARATOR = Comparator.comparing(Capability::getName);
    @SuppressWarnings("deprecation")
    private static final Comparator<CapabilityReferenceRecorder> CAPABILITY_REFERENCE_COMPARATOR = Comparator.comparing(CapabilityReferenceRecorder::getBaseDependentName);

    private final ResourceDescriptionResolver resolver;
    private Map<Capability, Predicate<ModelNode>> capabilities = Map.of();
    private List<AttributeDefinition> attributes = List.of();
    private Map<AttributeDefinition, OperationStepHandler> customAttributes = Map.of();
    private List<AttributeDefinition> ignoredAttributes = List.of();
    private List<AttributeDefinition> parameters = List.of();
    private Set<PathElement> requiredChildren = Set.of();
    private Set<PathElement> requiredSingletonChildren = Set.of();
    private Map<AttributeDefinition, AttributeTranslation> attributeTranslations = Map.of();
    private List<RuntimeResourceRegistration> runtimeResourceRegistrations = List.of();
    private Set<CapabilityReferenceRecorder> resourceCapabilityReferences = Set.of();
    private UnaryOperator<OperationStepHandler> addOperationTransformer = UnaryOperator.identity();
    private UnaryOperator<OperationStepHandler> operationTransformer = UnaryOperator.identity();
    private UnaryOperator<Resource> resourceTransformer = UnaryOperator.identity();

    public ResourceDescriptor(ResourceDescriptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public ResourceDescriptionResolver getDescriptionResolver() {
        return this.resolver;
    }

    @Override
    public Map<Capability, Predicate<ModelNode>> getCapabilities() {
        return this.capabilities;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return this.attributes;
    }

    @Override
    public Collection<AttributeDefinition> getIgnoredAttributes() {
        return this.ignoredAttributes;
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

    @Override
    public Map<AttributeDefinition, AttributeTranslation> getAttributeTranslations() {
        return this.attributeTranslations;
    }

    @Override
    public Map<AttributeDefinition, OperationStepHandler> getCustomAttributes() {
        return this.customAttributes;
    }

    @Override
    public Collection<RuntimeResourceRegistration> getRuntimeResourceRegistrations() {
        return this.runtimeResourceRegistrations;
    }

    @Override
    public Set<CapabilityReferenceRecorder> getResourceCapabilityReferences() {
        return this.resourceCapabilityReferences;
    }

    @Override
    public UnaryOperator<OperationStepHandler> getOperationTransformation() {
        return this.operationTransformer;
    }

    @Override
    public UnaryOperator<Resource> getResourceTransformation() {
        return this.resourceTransformer;
    }

    @Override
    public UnaryOperator<OperationStepHandler> getAddOperationTransformation() {
        return this.addOperationTransformer;
    }

    public ResourceDescriptor addAttribute(Attribute attribute, OperationStepHandler writeAttributeHandler) {
        return this.addAttribute(attribute.getDefinition(), writeAttributeHandler);
    }

    public ResourceDescriptor addAttribute(AttributeDefinition attribute, OperationStepHandler writeAttributeHandler) {
        if (this.customAttributes.isEmpty()) {
            this.customAttributes = Map.of(attribute, writeAttributeHandler);
        } else {
            if (this.customAttributes.size() == 1) {
                Map<AttributeDefinition, OperationStepHandler> existing = this.customAttributes;
                this.customAttributes = new TreeMap<>(ATTRIBUTE_COMPARATOR);
                this.customAttributes.putAll(existing);
            }
            this.customAttributes.put(attribute, writeAttributeHandler);
        }
        return this;
    }

    public <E extends Enum<E> & Attribute> ResourceDescriptor addAttributes(Class<E> enumClass) {
        return this.addAttributes(EnumSet.allOf(enumClass));
    }

    public ResourceDescriptor addAttributes(Set<? extends Attribute> attributes) {
        return this.addAttributes(attributes.stream());
    }

    public ResourceDescriptor addAttributes(Attribute... attributes) {
        return this.addAttributes(List.of(attributes).stream());
    }

    private ResourceDescriptor addAttributes(Stream<? extends Attribute> attributes) {
        return this.addAttributes(attributes.map(Attribute::getDefinition)::iterator);
    }

    public ResourceDescriptor addAttributes(AttributeDefinition... attributes) {
        return this.addAttributes(List.of(attributes));
    }

    public ResourceDescriptor addAttributes(Iterable<AttributeDefinition> attributes) {
        if (this.attributes.isEmpty()) {
            this.attributes = new LinkedList<>();
        }
        for (AttributeDefinition attribute : attributes) {
            this.attributes.add(attribute);
        }
        return this;
    }

    public <E extends Enum<E> & Attribute> ResourceDescriptor addIgnoredAttributes(Class<E> enumClass) {
        return this.addIgnoredAttributes(EnumSet.allOf(enumClass));
    }

    public ResourceDescriptor addIgnoredAttributes(Set<? extends Attribute> attributes) {
        return this.addIgnoredAttributes(attributes.stream());
    }

    public ResourceDescriptor addIgnoredAttributes(Attribute... attributes) {
        return this.addIgnoredAttributes(List.of(attributes).stream());
    }

    private ResourceDescriptor addIgnoredAttributes(Stream<? extends Attribute> attributes) {
        return this.addIgnoredAttributes(attributes.map(Attribute::getDefinition)::iterator);
    }

    public ResourceDescriptor addIgnoredAttributes(AttributeDefinition... attributes) {
        return this.addIgnoredAttributes(List.of(attributes));
    }

    public ResourceDescriptor addIgnoredAttributes(Iterable<AttributeDefinition> attributes) {
        if (this.ignoredAttributes.isEmpty()) {
            this.ignoredAttributes = new LinkedList<>();
        }
        for (AttributeDefinition attribute : attributes) {
            this.ignoredAttributes.add(attribute);
        }
        return this;
    }

    public <E extends Enum<E> & Attribute> ResourceDescriptor addExtraParameters(Class<E> enumClass) {
        return this.addExtraParameters(EnumSet.allOf(enumClass));
    }

    public ResourceDescriptor addExtraParameters(Set<? extends Attribute> parameters) {
        return this.addExtraParameters(parameters.stream());
    }

    public ResourceDescriptor addExtraParameters(Attribute... parameters) {
        return this.addExtraParameters(List.of(parameters).stream());
    }

    private ResourceDescriptor addExtraParameters(Stream<? extends Attribute> parameters) {
        return this.addExtraParameters(parameters.map(Attribute::getDefinition)::iterator);
    }

    public ResourceDescriptor addExtraParameters(AttributeDefinition... parameters) {
        return this.addExtraParameters(List.of(parameters));
    }

    public ResourceDescriptor addExtraParameters(Iterable<AttributeDefinition> parameters) {
        if (this.parameters.isEmpty()) {
            this.parameters = new LinkedList<>();
        }
        for (AttributeDefinition parameter : parameters) {
            this.parameters.add(parameter);
        }
        return this;
    }

    public <E extends Enum<E> & Capability> ResourceDescriptor addCapabilities(Class<E> enumClass) {
        return this.addCapabilities(Predicates.always(), enumClass);
    }

    public ResourceDescriptor addCapabilities(Capability... capabilities) {
        return this.addCapabilities(Predicates.always(), capabilities);
    }

    public ResourceDescriptor addCapabilities(Iterable<? extends Capability> capabilities) {
        return this.addCapabilities(Predicates.always(), capabilities);
    }

    public <E extends Enum<E> & Capability> ResourceDescriptor addCapabilities(Predicate<ModelNode> predicate, Class<E> enumClass) {
        return this.addCapabilities(predicate, EnumSet.allOf(enumClass));
    }

    public ResourceDescriptor addCapabilities(Predicate<ModelNode> predicate, Capability... capabilities) {
        return this.addCapabilities(predicate, Arrays.asList(capabilities));
    }

    public ResourceDescriptor addCapabilities(Predicate<ModelNode> predicate, Iterable<? extends Capability> capabilities) {
        if (this.capabilities.isEmpty()) {
            this.capabilities = new TreeMap<>(CAPABILITY_COMPARATOR);
        }
        for (Capability capability : capabilities) {
            this.capabilities.put(capability, predicate);
        }
        return this;
    }

    public <E extends Enum<E> & ResourceDefinitionProvider> ResourceDescriptor addRequiredChildren(Class<E> enumClass) {
        return this.addRequiredChildren(EnumSet.allOf(enumClass));
    }

    public ResourceDescriptor addRequiredChildren(Set<? extends ResourceDefinitionProvider> providers) {
        return this.addRequiredChildren(providers.stream().map(ResourceDefinitionProvider::getPathElement)::iterator);
    }

    public ResourceDescriptor addRequiredChildren(PathElement... paths) {
        return this.addRequiredChildren(List.of(paths));
    }

    public ResourceDescriptor addRequiredChildren(Iterable<PathElement> paths) {
        if (this.requiredChildren.isEmpty()) {
            this.requiredChildren = new TreeSet<>(PATH_COMPARATOR);
        }
        for (PathElement path : paths) {
            this.requiredChildren.add(path);
        }
        return this;
    }

    public <E extends Enum<E> & ResourceDefinition> ResourceDescriptor addRequiredSingletonChildren(Class<E> enumClass) {
        return this.addRequiredSingletonChildren(EnumSet.allOf(enumClass));
    }

    public ResourceDescriptor addRequiredSingletonChildren(Set<? extends ResourceDefinition> definitions) {
        return this.addRequiredSingletonChildren(definitions.stream().map(ResourceDefinition::getPathElement)::iterator);
    }

    public ResourceDescriptor addRequiredSingletonChildren(PathElement... paths) {
        return this.addRequiredSingletonChildren(List.of(paths));
    }

    public ResourceDescriptor addRequiredSingletonChildren(Iterable<PathElement> paths) {
        if (this.requiredSingletonChildren.isEmpty()) {
            this.requiredSingletonChildren = new TreeSet<>(PATH_COMPARATOR);
        }
        for (PathElement path : paths) {
            this.requiredSingletonChildren.add(path);
        }
        return this;
    }

    public ResourceDescriptor addAlias(Attribute alias, Attribute target) {
        return this.addAttributeTranslation(alias, () -> target);
    }

    public ResourceDescriptor addAttributeTranslation(Attribute sourceAttribute, AttributeTranslation translation) {
        if (this.attributeTranslations.isEmpty()) {
            this.attributeTranslations = Map.of(sourceAttribute.getDefinition(), translation);
        } else {
            if (this.attributeTranslations.size() == 1) {
                Map<AttributeDefinition, AttributeTranslation> existing = this.attributeTranslations;
                this.attributeTranslations = new TreeMap<>(ATTRIBUTE_COMPARATOR);
                this.attributeTranslations.putAll(existing);
            }
            this.attributeTranslations.put(sourceAttribute.getDefinition(), translation);
        }
        return this;
    }

    public ResourceDescriptor addRuntimeResourceRegistration(RuntimeResourceRegistration registration) {
        if (this.runtimeResourceRegistrations.isEmpty()) {
            this.runtimeResourceRegistrations = List.of(registration);
        } else {
            if (this.runtimeResourceRegistrations.size() == 1) {
                List<RuntimeResourceRegistration> existing = this.runtimeResourceRegistrations;
                this.runtimeResourceRegistrations = new LinkedList<>();
                this.runtimeResourceRegistrations.addAll(existing);
            }
            this.runtimeResourceRegistrations.add(registration);
        }
        return this;
    }

    public ResourceDescriptor addResourceCapabilityReference(CapabilityReferenceRecorder reference) {
        if (this.resourceCapabilityReferences.isEmpty()) {
            this.resourceCapabilityReferences = Set.of(reference);
        } else {
            if (this.resourceCapabilityReferences.size() == 1) {
                Set<CapabilityReferenceRecorder> existing = this.resourceCapabilityReferences;
                this.resourceCapabilityReferences = new TreeSet<>(CAPABILITY_REFERENCE_COMPARATOR);
                this.resourceCapabilityReferences.addAll(existing);
            }
            this.resourceCapabilityReferences.add(reference);
        }
        return this;
    }

    public ResourceDescriptor setAddOperationTransformation(UnaryOperator<OperationStepHandler> transformation) {
        this.addOperationTransformer = transformation;
        return this;
    }

    public ResourceDescriptor setOperationTransformation(UnaryOperator<OperationStepHandler> transformation) {
        this.operationTransformer = transformation;
        return this;
    }

    public ResourceDescriptor setResourceTransformation(UnaryOperator<Resource> transformation) {
        this.resourceTransformer = transformation;
        return this;
    }
}
