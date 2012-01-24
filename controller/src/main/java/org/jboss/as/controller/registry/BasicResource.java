/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.registry;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;

/**
 * Standard {@link Resource} implementation.
 *
 * <p>Concurrency note: if a thread needs to modify a BasicResource, it must use the clone() method to obtain its
 * own copy of the resource. That instance cannot be made visible to other threads until all writes are complete.</p>
 *
 * @author Emanuel Muckenhuber
 */
class BasicResource implements Resource {

    /** The local model. */
    private final ModelNode model = new ModelNode();
    /** The children. */
    private final Map<String, ResourceProvider> children = new LinkedHashMap<String, ResourceProvider>();

    protected BasicResource() {
    }

    @Override
    public ModelNode getModel() {
        return model;
    }

    @Override
    public void writeModel(ModelNode newModel) {
        model.set(newModel);
    }

    @Override
    public boolean isModelDefined() {
        return model.isDefined();
    }

    @Override
    public Resource getChild(final PathElement address) {
        final ResourceProvider provider = getProvider(address.getKey());
        if(provider == null) {
            return null;
        }
        return provider.get(address.getValue());
    }

    @Override
    public boolean hasChild(final PathElement address) {
        final ResourceProvider provider = getProvider(address.getKey());
        if(provider == null) {
            return false;
        }
        if(address.isWildcard()) {
            return provider.hasChildren();
        }
        return provider.has(address.getValue());
    }

    @Override
    public Resource requireChild(final PathElement address) {
        final Resource resource = getChild(address);
        if(resource == null) {
            throw new NoSuchResourceException(address);
        }
        return resource;
    }

    @Override
    public boolean hasChildren(final String childType) {
        final ResourceProvider provider = getProvider(childType);
        return provider != null && provider.hasChildren();
    }

    @Override
    public Resource navigate(final PathAddress address) {
        return Resource.Tools.navigate(this, address);
    }

    @Override
    public Set<String> getChildrenNames(final String childType) {
        final ResourceProvider provider = getProvider(childType);
        if(provider == null) {
            return Collections.emptySet();
        }
        return provider.children();
    }

    @Override
    public Set<String> getChildTypes() {
        synchronized (children) {
            return new LinkedHashSet<String>(children.keySet());
        }
    }

    @Override
    public Set<ResourceEntry> getChildren(final String childType) {
        final ResourceProvider provider = getProvider(childType);
        if(provider == null) {
            return Collections.emptySet();
        }
        final Set<ResourceEntry> children = new LinkedHashSet<ResourceEntry>();
        for(final String name : provider.children()) {
            final Resource resource = provider.get(name);
            children.add(new DelegateResource(resource) {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public PathElement getPathElement() {
                    return PathElement.pathElement(childType, name);
                }
            });
        }
        return children;
    }

    @Override
    public void registerChild(final PathElement address, final Resource resource) {
        if(address.isMultiTarget()) {
            throw new IllegalArgumentException();
        }
        getOrCreateProvider(address.getKey()).register(address.getValue(), resource);
    }

    @Override
    public Resource removeChild(PathElement address) {
        final ResourceProvider provider = getProvider(address.getKey());
        if(provider == null) {
            return null;
        }
        return provider.remove(address.getValue());
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public boolean isRuntime() {
        return false;
    }

    @SuppressWarnings({"CloneDoesntCallSuperClone"})
    @Override
    public Resource clone() {
        final Resource clone = new BasicResource();
        for (;;) {
            try {
                clone.writeModel(model);
                break;
            } catch (ConcurrentModificationException ignore) {
                // TODO horrible hack :(
            }
        }
        for(final String childType : getChildTypes()) {
            for(final ResourceEntry child : getChildren(childType)) {
                clone.registerChild(child.getPathElement(), child.clone());
            }
        }
        return clone;
    }

    protected void registerResourceProvider(final String type, final ResourceProvider provider) {
        synchronized (children) {
            if (children.containsKey(type)) {
                throw MESSAGES.duplicateResourceType(type);
            }
            children.put(type, provider);
        }
    }

    protected final ResourceProvider getProvider(final String type) {
        synchronized (children) {
            return children.get(type);
        }
    }

    protected ResourceProvider getOrCreateProvider(final String type) {
        synchronized (children) {
            final ResourceProvider provider = children.get(type);
            if(provider != null) {
                return provider;
            } else {
                final ResourceProvider newProvider = new DefaultResourceProvider();
                children.put(type, newProvider);
                return newProvider;
            }
        }
    }

    static class DefaultResourceProvider implements ResourceProvider {

        private final Map<String, Resource> children = new LinkedHashMap<String, Resource>();

        protected DefaultResourceProvider() {
        }

        @Override
        public Set<String> children() {
            synchronized (children) {
                return new LinkedHashSet<String>(children.keySet());
            }
        }

        @Override
        public boolean has(String name) {
            synchronized (children) {
                return children.get(name) != null;
            }
        }

        @Override
        public Resource get(String name) {
            synchronized (children) {
                return children.get(name);
            }
        }

        @Override
        public boolean hasChildren() {
            return ! children().isEmpty();
        }

        @Override
        public void register(String name, Resource resource) {
            synchronized (children) {
                if (children.containsKey(name)) {
                    throw MESSAGES.duplicateResource(name);
                }
                children.put(name, resource);
            }
        }

        @Override
        public Resource remove(String name) {
            synchronized (children) {
                return children.remove(name);
            }
        }
    }

    abstract static class DelegateResource implements ResourceEntry {
        final Resource delegate;
        protected DelegateResource(Resource delegate) {
            if(delegate == null) {
                throw new IllegalArgumentException();
            }
            this.delegate = delegate;
        }

        @Override
        public Resource getChild(PathElement element) {
            return delegate.getChild(element);
        }

        @Override
        public Set<ResourceEntry> getChildren(String childType) {
            return delegate.getChildren(childType);
        }

        @Override
        public Set<String> getChildrenNames(String childType) {
            return delegate.getChildrenNames(childType);
        }

        @Override
        public Set<String> getChildTypes() {
            return delegate.getChildTypes();
        }

        @Override
        public ModelNode getModel() {
            return delegate.getModel();
        }

        @Override
        public boolean hasChild(PathElement element) {
            return delegate.hasChild(element);
        }

        @Override
        public boolean hasChildren(String childType) {
            return delegate.hasChildren(childType);
        }

        @Override
        public boolean isModelDefined() {
            return delegate.isModelDefined();
        }

        @Override
        public Resource navigate(PathAddress address) {
            return delegate.navigate(address);
        }

        @Override
        public void registerChild(PathElement address, Resource resource) {
            delegate.registerChild(address, resource);
        }

        @Override
        public Resource removeChild(PathElement address) {
            return delegate.removeChild(address);
        }

        @Override
        public Resource requireChild(PathElement element) {
            return delegate.requireChild(element);
        }

        @Override
        public void writeModel(ModelNode newModel) {
            delegate.writeModel(newModel);
        }

        @Override
        public boolean isRuntime() {
            return delegate.isRuntime();
        }

        @Override
        public boolean isProxy() {
            return delegate.isProxy();
        }

        @SuppressWarnings({"CloneDoesntCallSuperClone"})
        @Override
        public Resource clone() {
           return delegate.clone();
        }
    }

}
