/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem.deployment;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.ResourceProvider;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.dmr.ModelNode;

/**
 * A combination of {@code org.jboss.as.controller.registry.BasicResource} and
 * {@code org.jboss.as.controller.registry.AbstractModelResource}, with some
 * modifications for ejb timer service resource, e.g., use {@code ConcurrentHashMap}
 * instead of {@code HashMap}.
 */
final class TimerServiceBasicResource extends ResourceProvider.ResourceProviderRegistry implements Resource {
    private final ModelNode model = new ModelNode();

    private final ConcurrentMap<String, ResourceProvider> children = new ConcurrentHashMap<>();

    @Override
    public ModelNode getModel() {
        return model;
    }

    @Override
    public void writeModel(final ModelNode newModel) {
        model.set(newModel);
    }

    @Override
    public boolean isModelDefined() {
        return model.isDefined();
    }

    @Override
    public Resource clone() {
        final TimerServiceBasicResource clone = new TimerServiceBasicResource();
        for (; ; ) {
            try {
                clone.writeModel(model);
                break;
            } catch (ConcurrentModificationException ignore) {
                // TODO horrible hack :(
            }
        }
        cloneProviders(clone);
        return clone;
    }

    @Override
    public Resource getChild(final PathElement address) {
        final ResourceProvider provider = getProvider(address.getKey());
        if (provider == null) {
            return null;
        }
        return provider.get(address.getValue());
    }

    @Override
    public boolean hasChild(final PathElement address) {
        final ResourceProvider provider = getProvider(address.getKey());
        if (provider == null) {
            return false;
        }
        if (address.isWildcard()) {
            return provider.hasChildren();
        }
        return provider.has(address.getValue());
    }

    @Override
    public Resource requireChild(final PathElement address) {
        final Resource resource = getChild(address);
        if (resource == null) {
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
        return Tools.navigate(this, address);
    }

    @Override
    public Set<String> getChildrenNames(final String childType) {
        final ResourceProvider provider = getProvider(childType);
        if (provider == null) {
            return Collections.emptySet();
        }
        return provider.children();
    }

    @Override
    public Set<String> getChildTypes() {
        return new LinkedHashSet<>(children.keySet());
    }

    @Override
    public Set<ResourceEntry> getChildren(final String childType) {
        final ResourceProvider provider = getProvider(childType);
        if (provider == null) {
            return Collections.emptySet();
        }
        final Set<ResourceEntry> children = new LinkedHashSet<ResourceEntry>();
        for (final String name : provider.children()) {
            final Resource resource = provider.get(name);
            if (resource != null) {
                children.add(new TimerServiceBasicResource.DelegateResource(resource, PathElement.pathElement(childType, name)));
            }
        }
        return children;
    }

    @Override
    public void registerChild(final PathElement address, final Resource resource) {
        if (address.isMultiTarget()) {
            throw new IllegalArgumentException();
        }
        getOrCreateProvider(address.getKey()).register(address.getValue(), resource);
    }

    @Override
    public void registerChild(final PathElement address, final int index, final Resource resource) {
        throw EjbLogger.ROOT_LOGGER.indexedChildResourceRegistrationNotAvailable(address);
    }

    @Override
    public Resource removeChild(PathElement address) {
        final Resource[] removed = new Resource[1];
        children.computeIfPresent(address.getKey(), (k, v) -> {
            removed[0] = v.remove(address.getValue());

            // Cleanup default resource providers
            if ((v instanceof TimerServiceBasicResource.DefaultResourceProvider) && !v.hasChildren()) {
                return null;
            }
            return v;
        });
        return removed[0];
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    @Override
    public Set<String> getOrderedChildTypes() {
        return Collections.emptySet();
    }

    @Override
    protected void registerResourceProvider(final String type, final ResourceProvider provider) {
        final ResourceProvider previousValue = children.putIfAbsent(type, provider);
        if (previousValue != null) {
            throw ControllerLogger.ROOT_LOGGER.duplicateResourceType(type);
        }
    }

    private ResourceProvider getProvider(final String type) {
        return children.get(type);
    }

    private ResourceProvider getOrCreateProvider(final String type) {
        return children.computeIfAbsent(type, k -> new TimerServiceBasicResource.DefaultResourceProvider());
    }

    private void cloneProviders(TimerServiceBasicResource clone) {
        for (final Map.Entry<String, ResourceProvider> entry : children.entrySet()) {
            clone.registerResourceProvider(entry.getKey(), entry.getValue().clone());
        }
    }

    private static final class DefaultResourceProvider implements ResourceProvider {

        private final ConcurrentMap<String, Resource> children = new ConcurrentHashMap<>();

        @Override
        public Set<String> children() {
            return new LinkedHashSet<>(children.keySet());
        }

        @Override
        public boolean has(String name) {
            return children.get(name) != null;
        }

        @Override
        public Resource get(String name) {
            return children.get(name);
        }

        @Override
        public boolean hasChildren() {
            return !children.isEmpty();
        }

        @Override
        public void register(String name, Resource resource) {
            final Resource previousValue = children.putIfAbsent(name, resource);
            if (previousValue != null) {
                throw ControllerLogger.ROOT_LOGGER.duplicateResource(name);
            }
        }

        @Override
        public void register(String name, int index, Resource resource) {
            throw EjbLogger.ROOT_LOGGER.indexedChildResourceRegistrationNotAvailable(PathElement.pathElement(name));
        }

        @Override
        public Resource remove(String name) {
            return children.remove(name);
        }

        @Override
        public ResourceProvider clone() {
            final TimerServiceBasicResource.DefaultResourceProvider provider = new TimerServiceBasicResource.DefaultResourceProvider();
            for (final Map.Entry<String, Resource> entry : children.entrySet()) {
                provider.register(entry.getKey(), entry.getValue().clone());
            }
            return provider;
        }
    }

    private static final class DelegateResource implements ResourceEntry {
        private final PathElement pathElement;
        private final Resource delegate;

        private DelegateResource(Resource delegate, PathElement pathElement) {
            this.delegate = checkNotNullParam("delegate", delegate);
            this.pathElement = pathElement;
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
        public void registerChild(PathElement address, int index, Resource resource) {
            delegate.registerChild(address, index, resource);
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

        public Set<String> getOrderedChildTypes() {
            return delegate.getOrderedChildTypes();
        }

        @SuppressWarnings({"CloneDoesntCallSuperClone"})
        @Override
        public Resource clone() {
            return delegate.clone();
        }

        @Override
        public String getName() {
            return this.pathElement.getValue();
        }

        @Override
        public PathElement getPathElement() {
            return this.pathElement;
        }

        @Override
        public int hashCode() {
            return this.getPathElement().hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof ResourceEntry)) return false;
            return this.getPathElement().equals(((ResourceEntry) object).getPathElement());
        }
    }
}
