/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.DelegatingResource;
import org.jboss.as.controller.registry.Resource;

/**
 * A generic {@link Resource} decorator augmented to support additional runtime children.
 * @author Paul Ferraro
 */
public class ComplexResource extends DelegatingResource implements Function<String, ChildResourceProvider> {

    private final Map<String, ChildResourceProvider> providers;
    private final BiFunction<Resource, Map<String, ChildResourceProvider>, Resource> factory;

    /**
     * Constructs a new resource.
     * @param resource the concrete resource
     * @param providers a set of providers for specific child types
     */
    public ComplexResource(Resource resource, Map<String, ChildResourceProvider> providers) {
        this(resource, providers, ComplexResource::new);
    }

    /**
     * Constructs a new resource.
     * @param resource the concrete resource
     * @param providers a set of providers for specific child types
     * @param factory a function used to clone this resource
     */
    protected ComplexResource(Resource resource, Map<String, ChildResourceProvider> providers, BiFunction<Resource, Map<String, ChildResourceProvider>, Resource> factory) {
        super(resource);
        this.providers = providers;
        this.factory = factory;
    }

    @Override
    public ChildResourceProvider apply(String childType) {
        return this.providers.get(childType);
    }

    @Override
    public Resource clone() {
        return this.factory.apply(super.clone(), this.providers);
    }

    @Override
    public Resource getChild(PathElement path) {
        ChildResourceProvider provider = this.apply(path.getKey());
        return (provider != null) ? provider.getChild(path.getValue()) : super.getChild(path);
    }

    @Override
    public Set<Resource.ResourceEntry> getChildren(String childType) {
        ChildResourceProvider provider = this.apply(childType);
        if (provider != null) {
            Set<String> names = provider.getChildren();
            Set<Resource.ResourceEntry> entries = !names.isEmpty() ? new HashSet<>() : Collections.emptySet();
            for (String name : names) {
                Resource resource = provider.getChild(name);
                entries.add(new SimpleResourceEntry(PathElement.pathElement(childType, name), resource));
            }
            return entries;
        }
        return super.getChildren(childType);
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        ChildResourceProvider provider = this.apply(childType);
        return (provider != null) ? provider.getChildren() : super.getChildrenNames(childType);
    }

    @Override
    public Set<String> getChildTypes() {
        Set<String> childTypes = new HashSet<>(super.getChildTypes());
        childTypes.addAll(this.providers.keySet());
        return childTypes;
    }

    @Override
    public boolean hasChild(PathElement path) {
        ChildResourceProvider provider = this.apply(path.getKey());
        return (provider != null) ? provider.getChild(path.getValue()) != null : super.hasChild(path);
    }

    @Override
    public boolean hasChildren(String childType) {
        ChildResourceProvider provider = this.apply(childType);
        return (provider != null) ? !provider.getChildren().isEmpty() : super.hasChildren(childType);
    }

    @Override
    public Resource navigate(PathAddress address) {
        return (address.size() == 1) ? this.requireChild(address.getLastElement()) : super.navigate(address);
    }

    @Override
    public Resource requireChild(PathElement path) {
        Resource resource = this.getChild(path);
        if (resource == null) {
            throw new NoSuchResourceException(path);
        }
        return resource;
    }

    private static class SimpleResourceEntry extends DelegatingResource implements Resource.ResourceEntry {

        private final PathElement path;

        SimpleResourceEntry(PathElement path, Resource resource) {
            super(resource);
            this.path = path;
        }

        @Override
        public String getName() {
            return this.path.getValue();
        }

        @Override
        public PathElement getPathElement() {
            return this.path;
        }

        @Override
        public int hashCode() {
            return this.path.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Resource.ResourceEntry)) return false;
            return this.path.equals(((Resource.ResourceEntry) object).getPathElement());
        }

        @Override
        public String toString() {
            return this.path.toString();
        }
    }
}
