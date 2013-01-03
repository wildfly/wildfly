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

package org.jboss.as.controller.registry;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Versioned operation transformer registry.
 *
 * @author Emanuel Muckenhuber
 */
public class GlobalTransformerRegistry {

    private volatile Map<String, SubRegistry> subRegistries;
    private volatile Map<ModelVersion, OperationTransformerRegistry> versionedRegistries;

    private static final AtomicMapFieldUpdater<GlobalTransformerRegistry, String, SubRegistry> subRegistriesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(GlobalTransformerRegistry.class, Map.class, "subRegistries"));
    private static final AtomicMapFieldUpdater<GlobalTransformerRegistry, ModelVersion, OperationTransformerRegistry> registryUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(GlobalTransformerRegistry.class, Map.class, "versionedRegistries"));

    public GlobalTransformerRegistry() {
        registryUpdater.clear(this);
        subRegistriesUpdater.clear(this);
    }

    /**
     * Discard an operation.
     *
     * @param address the operation handler address
     * @param major the major version
     * @param minor the minor version
     * @param operationName the operation name
     */
    public void discardOperation(final PathAddress address, int major, int minor, final String operationName) {
        registerTransformer(address.iterator(), ModelVersion.create(major, minor), operationName, OperationTransformerRegistry.DISCARD);
    }

    /**
     * Discard an operation.
     *
     * @param address the operation handler address
     * @param version the model version
     * @param operationName the operation name
     */
    public void discardOperation(final PathAddress address, ModelVersion version, final String operationName) {
        registerTransformer(address.iterator(), version, operationName, OperationTransformerRegistry.DISCARD);
    }

    /**
     * Register an operation transformer.
     *
     * @param address the operation handler address
     * @param major the major version
     * @param minor the minor version
     * @param operationName the operation name
     * @param transformer the operation transformer
     */
    public void registerTransformer(final PathAddress address, int major, int minor, String operationName, OperationTransformer transformer) {
        registerTransformer(address.iterator(), ModelVersion.create(major, minor), operationName, new OperationTransformerRegistry.OperationTransformerEntry(transformer, OperationTransformerRegistry.TransformationPolicy.TRANSFORM));
    }

    public void createDiscardingChildRegistry(final PathAddress address, final ModelVersion version) {
        createChildRegistry(address.iterator(), version, DISCARD, OperationTransformerRegistry.DISCARD);
    }

    public void createChildRegistry(final PathAddress address, final ModelVersion version, OperationTransformer transformer) {
        createChildRegistry(address.iterator(), version, RESOURCE_TRANSFORMER, new OperationTransformerRegistry.OperationTransformerEntry(transformer, OperationTransformerRegistry.TransformationPolicy.TRANSFORM));
    }

    public void createChildRegistry(final PathAddress address, final ModelVersion version, ResourceTransformer resourceTransformer, boolean inherited) {
        createChildRegistry(address.iterator(), version, new OperationTransformerRegistry.ResourceTransformerEntry(resourceTransformer, inherited), OperationTransformerRegistry.FORWARD);
    }

    public void createChildRegistry(final PathAddress address, final ModelVersion version, ResourceTransformer resourceTransformer, OperationTransformer operationTransformer) {
        createChildRegistry(address.iterator(), version, new OperationTransformerRegistry.ResourceTransformerEntry(resourceTransformer, false), new OperationTransformerRegistry.OperationTransformerEntry(operationTransformer, OperationTransformerRegistry.TransformationPolicy.TRANSFORM));
    }

    /**
     * Register an operation transformer.
     *
     * @param address the operation handler address
     * @param version the model version
     * @param operationName the operation name
     * @param transformer the operation transformer
     */
    public void registerTransformer(final PathAddress address, final ModelVersion version, String operationName, OperationTransformer transformer) {
        registerTransformer(address.iterator(), version, operationName, new OperationTransformerRegistry.OperationTransformerEntry(transformer, OperationTransformerRegistry.TransformationPolicy.TRANSFORM));
    }

    public OperationTransformerRegistry mergeSubtree(final OperationTransformerRegistry parent, final PathAddress address, final Map<PathAddress, ModelVersion> subTree) {
        final OperationTransformerRegistry target = parent.createChildRegistry(address.iterator(), RESOURCE_TRANSFORMER, OperationTransformerRegistry.FORWARD);
        mergeSubtree(target, subTree);
        return target;
    }

    /**
     * Merge a subtree.
     *
     * @param targetRegistry the target registry
     * @param subTree the subtree
     */
    public void mergeSubtree(final OperationTransformerRegistry targetRegistry, final Map<PathAddress, ModelVersion> subTree) {
        for(Map.Entry<PathAddress, ModelVersion> entry: subTree.entrySet()) {
            mergeSubtree(targetRegistry, entry.getKey(), entry.getValue());
        }
    }

    protected void mergeSubtree(final OperationTransformerRegistry targetRegistry, final PathAddress address, final ModelVersion version) {
        final GlobalTransformerRegistry child = navigate(address.iterator());
        if(child != null) {
            child.process(targetRegistry, address, version, Collections.<PathAddress, ModelVersion>emptyMap());
        }
    }

    public OperationTransformerRegistry create(final ModelVersion version, final Map<PathAddress, ModelVersion> versions) {
        final OperationTransformerRegistry registry = new OperationTransformerRegistry(RESOURCE_TRANSFORMER, null);
        process(registry, PathAddress.EMPTY_ADDRESS, version, versions);
        return registry;
    }

    protected void process(final OperationTransformerRegistry registry, final PathAddress address, final ModelVersion version, Map<PathAddress, ModelVersion> versions) {
        final OperationTransformerRegistry current = registryUpdater.get(this, version);
        if(current != null) {
            final OperationTransformerRegistry.ResourceTransformerEntry resourceTransformer = current.getResourceTransformer();
            final OperationTransformerRegistry.OperationTransformerEntry defaultTransformer = current.getDefaultTransformer();
            registry.createChildRegistry(address.iterator(), resourceTransformer, defaultTransformer);
            final Map<String, OperationTransformerRegistry.OperationTransformerEntry> transformers = current.getTransformers();
            for(final Map.Entry<String, OperationTransformerRegistry.OperationTransformerEntry> transformer : transformers.entrySet()) {
                registry.registerTransformer(address, transformer.getKey(), transformer.getValue().getTransformer());
            }
        }
        final Map<String, SubRegistry> snapshot = subRegistriesUpdater.get(this);
        if(snapshot != null) {
            for(final Map.Entry<String, SubRegistry> registryEntry : snapshot.entrySet()) {
                //
                final String key = registryEntry.getKey();
                final SubRegistry subRegistry = registryEntry.getValue();
                final Map<String, GlobalTransformerRegistry> children = SubRegistry.childrenUpdater.get(subRegistry);
                for(final Map.Entry<String, GlobalTransformerRegistry> childEntry : children.entrySet()) {
                    //
                    final String value = childEntry.getKey();
                    final GlobalTransformerRegistry child = childEntry.getValue();
                    final PathAddress childAddress = address.append(PathElement.pathElement(key, value));
                    final ModelVersion childVersion = versions.containsKey(childAddress) ? versions.get(childAddress) : version;
                    child.process(registry, childAddress, childVersion, versions);
                }
            }
        }
    }

    protected void createChildRegistry(final Iterator<PathElement> iterator, ModelVersion version, OperationTransformerRegistry.ResourceTransformerEntry resourceTransformer, OperationTransformerRegistry.OperationTransformerEntry entry) {
        if(! iterator.hasNext()) {
            getOrCreate(version, resourceTransformer, entry);
        } else {
            final PathElement element = iterator.next();
            getOrCreate(element.getKey()).getOrCreate(element.getValue()).createChildRegistry(iterator, version, resourceTransformer, entry);
        }
    }

    protected void registerTransformer(final Iterator<PathElement> iterator, ModelVersion version, String operationName, OperationTransformerRegistry.OperationTransformerEntry entry) {
        if(! iterator.hasNext()) {
            // by default skip the default transformer
            getOrCreate(version, null, null).registerTransformer(PathAddress.EMPTY_ADDRESS.iterator(), operationName, entry);
        } else {
            final PathElement element = iterator.next();
            final SubRegistry subRegistry = getOrCreate(element.getKey());
            subRegistry.registerTransformer(iterator, element.getValue(), version, operationName,   entry);
        }
    }

    protected OperationTransformerRegistry.OperationTransformerEntry resolveTransformer(final Iterator<PathElement> iterator, ModelVersion version, String operationName) {
        if(!iterator.hasNext()) {
            final OperationTransformerRegistry registry = registryUpdater.get(this, version);
            if(registry == null) {
                return null;
            }
            return registry.resolveOperationTransformer(PathAddress.EMPTY_ADDRESS, operationName);
        } else {
            final PathElement element = iterator.next();
            final SubRegistry registry = subRegistriesUpdater.get(this, element.getKey());
            if(registry == null) {
                return null;
            }
            return registry.resolveTransformer(iterator, element.getValue(), version, operationName);
        }
    }

    GlobalTransformerRegistry navigate(final Iterator<PathElement> iterator) {
        if(! iterator.hasNext()) {
            return this;
        } else {
            final PathElement element = iterator.next();
            final SubRegistry registry = subRegistriesUpdater.get(this, element.getKey());
            if(registry == null) {
                return null;
            }
            GlobalTransformerRegistry other = SubRegistry.childrenUpdater.get(registry, element.getValue());
            if(other != null) {
                return other.navigate(iterator);
            }
            return null;
        }
    };

    SubRegistry getOrCreate(final String key) {
        for (;;) {
            final Map<String, SubRegistry> subRegistries = subRegistriesUpdater.get(this);
            SubRegistry registry = subRegistries.get(key);
            if(registry == null) {
                registry = new SubRegistry();
                SubRegistry existing = subRegistriesUpdater.putAtomic(this, key, registry, subRegistries);
                if(existing == null) {
                    return registry;
                } else if (existing != registry) {
                    return existing;
                }
            }
            return registry;
        }
    }

    OperationTransformerRegistry getOrCreate(final ModelVersion version, OperationTransformerRegistry.ResourceTransformerEntry resourceTransformer, final OperationTransformerRegistry.OperationTransformerEntry defaultTransformer) {
        for(;;) {
            final Map<ModelVersion, OperationTransformerRegistry> snapshot = registryUpdater.get(this);
            OperationTransformerRegistry registry = snapshot.get(version);
            if(registry == null) {
                registry = new OperationTransformerRegistry(resourceTransformer, defaultTransformer);
                OperationTransformerRegistry existing = registryUpdater.putAtomic(this, version, registry, snapshot);
                if(existing == null) {
                    return registry;
                } else if (existing != registry) {
                    return existing;
                }
            }
            return registry;
        }
    }

    static class SubRegistry {

        private static final AtomicMapFieldUpdater<SubRegistry, String, GlobalTransformerRegistry> childrenUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(SubRegistry.class, Map.class, "children"));
        private volatile Map<String, GlobalTransformerRegistry> children;

        SubRegistry() {
            childrenUpdater.clear(this);
        }

        GlobalTransformerRegistry getOrCreate(final String value) {
            for(;;) {
                final Map<String, GlobalTransformerRegistry> entries = childrenUpdater.get(this);
                GlobalTransformerRegistry entry = entries.get(value);
                if(entry != null) {
                    return entry;
                } else {
                    entry = new GlobalTransformerRegistry();
                    final GlobalTransformerRegistry existing = childrenUpdater.putAtomic(this, value, entry, entries);
                    if(existing == null) {
                        return entry;
                    } else if(existing != entry) {
                        return existing;
                    }
                }
            }
        }

        public OperationTransformerRegistry.OperationTransformerEntry resolveTransformer(Iterator<PathElement> iterator, String value, ModelVersion version, String operationName) {
            final GlobalTransformerRegistry registry = childrenUpdater.get(this, value);
            if(registry == null) {
                return null;
            }
            return registry.resolveTransformer(iterator, version, operationName);
        }

        public void registerTransformer(Iterator<PathElement> iterator, String value, ModelVersion version, String operationName, OperationTransformerRegistry.OperationTransformerEntry entry) {
            getOrCreate(value).registerTransformer(iterator, version, operationName, entry);
        }
    }

    static OperationTransformerRegistry.ResourceTransformerEntry RESOURCE_TRANSFORMER = new OperationTransformerRegistry.ResourceTransformerEntry(ResourceTransformer.DEFAULT, false);
    static OperationTransformerRegistry.ResourceTransformerEntry DISCARD = new OperationTransformerRegistry.ResourceTransformerEntry(ResourceTransformer.DISCARD, false);

}
