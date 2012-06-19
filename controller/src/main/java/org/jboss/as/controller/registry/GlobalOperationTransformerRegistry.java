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
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Versioned operation transformer registry.
 *
 * @author Emanuel Muckenhuber
 */
public class GlobalOperationTransformerRegistry {

    private volatile Map<String, SubRegistry> subRegistries;
    private volatile Map<ModelVersion, OperationTransformerRegistry> versionedRegistries;

    private static final AtomicMapFieldUpdater<GlobalOperationTransformerRegistry, String, SubRegistry> subRegistriesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(GlobalOperationTransformerRegistry.class, Map.class, "subRegistries"));
    private static final AtomicMapFieldUpdater<GlobalOperationTransformerRegistry, ModelVersion, OperationTransformerRegistry> registryUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(GlobalOperationTransformerRegistry.class, Map.class, "versionedRegistries"));

    public GlobalOperationTransformerRegistry() {
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
        registerTransformer(address.iterator(), ModelVersion.create(major, minor), operationName, new OperationTransformerRegistry.TransformerEntry(transformer, OperationTransformerRegistry.TransformationPolicy.TRANSFORM));
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
        registerTransformer(address.iterator(), version, operationName, new OperationTransformerRegistry.TransformerEntry(transformer, OperationTransformerRegistry.TransformationPolicy.TRANSFORM));
    }

    /**
     * Resolve the operation transformers for a given version.
     *
     * @param major the major version
     * @param minor the minor version
     * @param subsystems the subsystem versions
     * @return the resolved operation registry
     */
    public OperationTransformerRegistry resolve(final int major, final int minor, final ModelNode subsystems) {
        return resolve(ModelVersion.create(major, minor), subsystems);
    }

    /**
     * Resolve the operation transformers for a given version.
     *
     * @param version the model version
     * @param subsystems the subsystems
     * @return the resolved operation registry
     */
    public OperationTransformerRegistry resolve(final ModelVersion version, final ModelNode subsystems) {
        final PathAddress base = PathAddress.EMPTY_ADDRESS;
        final Map<PathAddress, ModelVersion> versions = new HashMap<PathAddress, ModelVersion>();
        for(final Property property : subsystems.asPropertyList()) {
            final String name = property.getName();
            final PathAddress address = base.append(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, name));
            versions.put(address, convert(property.getValue().asString()));
        }
        return create(version, versions);
    }

    protected OperationTransformerRegistry create(final ModelVersion version, final Map<PathAddress, ModelVersion> versions) {
        final OperationTransformerRegistry registry = new OperationTransformerRegistry();
        process(registry, PathAddress.EMPTY_ADDRESS, version, versions);
        return registry;
    }

    protected void process(final OperationTransformerRegistry registry, final PathAddress address, final ModelVersion version, Map<PathAddress, ModelVersion> versions) {
        final OperationTransformerRegistry current = registryUpdater.get(this, version);
        if(current != null) {
            final Map<String, OperationTransformerRegistry.TransformerEntry> transformers = current.getTransformers();
            for(final Map.Entry<String, OperationTransformerRegistry.TransformerEntry> transformer : transformers.entrySet()) {
                registry.registerTransformer(address, transformer.getKey(), transformer.getValue().getTransformer());
            }
        }
        final Map<String, SubRegistry> snapshot = subRegistriesUpdater.get(this);
        if(snapshot != null) {
            for(final Map.Entry<String, SubRegistry> registryEntry : snapshot.entrySet()) {
                //
                final String key = registryEntry.getKey();
                final SubRegistry subRegistry = registryEntry.getValue();
                final Map<String, GlobalOperationTransformerRegistry> children = SubRegistry.childrenUpdater.get(subRegistry);
                for(final Map.Entry<String, GlobalOperationTransformerRegistry> childEntry : children.entrySet()) {
                    //
                    final String value = childEntry.getKey();
                    final GlobalOperationTransformerRegistry child = childEntry.getValue();
                    final PathAddress childAddress = address.append(PathElement.pathElement(key, value));
                    final ModelVersion childVersion = versions.containsKey(childAddress) ? versions.get(childAddress) : version;
                    child.process(registry, childAddress, childVersion, versions);
                }
            }
        }
    }

    protected void registerTransformer(final Iterator<PathElement> iterator, ModelVersion version, String operationName, OperationTransformerRegistry.TransformerEntry entry) {
        if(! iterator.hasNext()) {
            getOrCreate(version).registerTransformer(PathAddress.EMPTY_ADDRESS.iterator(), operationName, entry);
        } else {
            final PathElement element = iterator.next();
            final SubRegistry subRegistry = getOrCreate(element.getKey());
            subRegistry.registerTransformer(iterator, element.getValue(), version, operationName,   entry);
        }
    }

    protected OperationTransformerRegistry.TransformerEntry resolveTransformer(final Iterator<PathElement> iterator, ModelVersion version, String operationName) {
        if(!iterator.hasNext()) {
            final OperationTransformerRegistry registry = registryUpdater.get(this, version);
            if(registry == null) {
                return null;
            }
            return registry.resolveTransformer(PathAddress.EMPTY_ADDRESS, operationName);
        } else {
            final PathElement element = iterator.next();
            final SubRegistry registry = subRegistriesUpdater.get(this, element.getKey());
            if(registry == null) {
                return null;
            }
            return registry.resolveTransformer(iterator, element.getValue(), version, operationName);
        }
    }

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

    OperationTransformerRegistry getOrCreate(final ModelVersion version) {
        for(;;) {
            final Map<ModelVersion, OperationTransformerRegistry> snapshot = registryUpdater.get(this);
            OperationTransformerRegistry registry = snapshot.get(version);
            if(registry == null) {
                registry = new OperationTransformerRegistry();
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

        private static final AtomicMapFieldUpdater<SubRegistry, String, GlobalOperationTransformerRegistry> childrenUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(SubRegistry.class, Map.class, "children"));
        private volatile Map<String, GlobalOperationTransformerRegistry> children;

        SubRegistry() {
            childrenUpdater.clear(this);
        }

        GlobalOperationTransformerRegistry getOrCreate(final String value) {
            for(;;) {
                final Map<String, GlobalOperationTransformerRegistry> entries = childrenUpdater.get(this);
                GlobalOperationTransformerRegistry entry = entries.get(value);
                if(entry != null) {
                    return entry;
                } else {
                    entry = new GlobalOperationTransformerRegistry();
                    final GlobalOperationTransformerRegistry existing = childrenUpdater.putAtomic(this, value, entry, entries);
                    if(existing == null) {
                        return entry;
                    } else if(existing != entry) {
                        return existing;
                    }
                }
            }
        }

        public OperationTransformerRegistry.TransformerEntry resolveTransformer(Iterator<PathElement> iterator, String value, ModelVersion version, String operationName) {
            final GlobalOperationTransformerRegistry registry = childrenUpdater.get(this, value);
            if(registry == null) {
                return null;
            }
            return registry.resolveTransformer(iterator, version, operationName);
        }

        public void registerTransformer(Iterator<PathElement> iterator, String value, ModelVersion version, String operationName, OperationTransformerRegistry.TransformerEntry entry) {
            getOrCreate(value).registerTransformer(iterator, version, operationName, entry);
        }
    }

    static ModelVersion convert(final String version) {
        final String[] s = version.split("\\.");
        final int length = s.length;
        if(length > 3) {
            throw new IllegalStateException();
        }
        int major = Integer.valueOf(s[0]);
        int minor = length > 1 ? Integer.valueOf(s[1]) : 0;
        int micro = length == 3 ? Integer.valueOf(s[2]) : 0;
        return ModelVersion.create(major, minor, micro);
    }

}
