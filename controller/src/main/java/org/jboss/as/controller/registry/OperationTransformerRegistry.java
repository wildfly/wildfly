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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WILDCARD;

import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathTransformation;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformerEntry;
import org.jboss.dmr.ModelNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Resolved/unversioned operation transformer registry.
 *
 * @author Emanuel Muckenhuber
 */
public class OperationTransformerRegistry {

    private final PathTransformation pathTransformation;
    private final ResourceTransformerEntry resourceTransformer;
    private final OperationTransformerEntry defaultTransformer;
    private volatile Map<String, SubRegistry> subRegistries;
    private volatile Map<String, OperationTransformerEntry> transformerEntries;

    private static final AtomicMapFieldUpdater<OperationTransformerRegistry, String, SubRegistry> subRegistriesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(OperationTransformerRegistry.class, Map.class, "subRegistries"));
    private static final AtomicMapFieldUpdater<OperationTransformerRegistry, String, OperationTransformerEntry> entriesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(OperationTransformerRegistry.class, Map.class, "transformerEntries"));

    protected OperationTransformerRegistry(final ResourceTransformerEntry resourceTransformer, final OperationTransformerEntry defaultTransformer) {
        this(PathTransformation.DEFAULT, resourceTransformer, defaultTransformer);
    }

    protected OperationTransformerRegistry(final PathTransformation pathTransformation,  ResourceTransformerEntry resourceTransformer, final OperationTransformerEntry defaultTransformer) {
        entriesUpdater.clear(this);
        subRegistriesUpdater.clear(this);
        this.defaultTransformer = defaultTransformer;
        this.resourceTransformer = resourceTransformer;
        this.pathTransformation = pathTransformation;
    }

    public TransformerEntry getTransformerEntry(final PathAddress address) {
        return resolveTransformerEntry(address.iterator());
    }

    protected TransformerEntry getTransformerEntry() {
        return new TransformerEntry() {
            @Override
            public PathTransformation getPathTransformation() {
                return pathTransformation;
            }

            @Override
            public ResourceTransformer getResourceTransformer() {
                return resourceTransformer.getTransformer();
            }

            @Override
            public OperationTransformer getOperationTransformer() {
                return defaultTransformer.getTransformer();
            }
        };
    }

    /**
     * Resolve a resource transformer for a given address.
     *
     * @param address the address
     * @return the resource transformer
     */
    public ResourceTransformerEntry resolveResourceTransformer(final PathAddress address) {
        return resolveResourceTransformer(address.iterator(), null);
    }

    /**
     * Resolve an operation transformer entry.
     *
     * @param address the address
     * @param operationName the operation name
     * @return the transformer entry
     */
    public OperationTransformerEntry resolveOperationTransformer(final PathAddress address, final String operationName) {
        final Iterator<PathElement> iterator = address.iterator();
        final OperationTransformerEntry entry = resolveTransformer(iterator, operationName);
        if(entry != null) {
            return entry;
        }
        // Default is forward unchanged
        return FORWARD;
    }

    /**
     * Merge a new subsystem from the global registration.
     *
     * @param registry the global registry
     * @param subsystemName the subsystem name
     * @param version the subsystem version
     */
    public void mergeSubsystem(final GlobalTransformerRegistry registry, String subsystemName, ModelVersion version) {
        final PathElement element = PathElement.pathElement(SUBSYSTEM, subsystemName);
        registry.mergeSubtree(this, PathAddress.EMPTY_ADDRESS.append(element), version);
    }

    /**
     * Get a list of path transformers for a given address.
     *
     * @param address the path address
     * @return a list of path transformations
     */
    public List<PathTransformation> getPathTransformations(final PathAddress address) {
        final List<PathTransformation> list = new ArrayList<PathTransformation>();
        final Iterator<PathElement> iterator = address.iterator();
        resolvePathTransformers(iterator, list);
        if(iterator.hasNext()) {
            while(iterator.hasNext()) {
                iterator.next();
                list.add(PathTransformation.DEFAULT);
            }
        }
        return list;
    }

    public OperationTransformerRegistry getChild(final PathAddress address) {
        final Iterator<PathElement> iterator = address.iterator();
        return resolveChild(iterator);
    }

    protected TransformerEntry resolveTransformerEntry(Iterator<PathElement> iterator) {
        if(!iterator.hasNext()) {
            return getTransformerEntry();
        } else {
            final PathElement element = iterator.next();
            SubRegistry sub = subRegistriesUpdater.get(this, element.getKey());
            if(sub == null) {
                return null;
            }
            final OperationTransformerRegistry registry = sub.get(element.getValue());
            if(registry == null) {
                return null;
            }
            return registry.resolveTransformerEntry(iterator);
        }
    }

    protected ResourceTransformerEntry getResourceTransformer() {
        return resourceTransformer;
    }

    protected OperationTransformerRegistry resolveChild(final Iterator<PathElement> iterator) {

        if(! iterator.hasNext()) {
            return this;
        } else {
            final PathElement element = iterator.next();
            SubRegistry sub = subRegistriesUpdater.get(this, element.getKey());
            if(sub == null) {
                return null;
            }
            return sub.get(element.getValue(), iterator);
        }
    }

    protected void resolvePathTransformers(Iterator<PathElement> iterator, List<PathTransformation> list) {
        list.add(pathTransformation);
        if(iterator.hasNext()) {
            final PathElement element = iterator.next();
            SubRegistry sub = subRegistriesUpdater.get(this, element.getKey());
            if(sub != null) {
                final OperationTransformerRegistry reg = sub.get(element.getValue());
                if(reg != null) {
                    reg.resolvePathTransformers(iterator, list);
                    return;
                }
            }
            list.add(PathTransformation.DEFAULT);
            return;
        }
    }

    protected void registerTransformer(final PathAddress address, final String operationName, final OperationTransformer transformer) {
        registerTransformer(address.iterator(), operationName, new OperationTransformerEntry(transformer, TransformationPolicy.TRANSFORM));
    }

    public OperationTransformerEntry getDefaultTransformer() {
        return defaultTransformer;
    }

    protected Map<String, OperationTransformerEntry> getTransformers() {
        return entriesUpdater.get(this);
    }

    protected OperationTransformerRegistry createChildRegistry(final Iterator<PathElement> iterator,  ResourceTransformerEntry resourceTransformer, OperationTransformerEntry defaultTransformer) {
        if(!iterator.hasNext()) {
            return this;
        } else {
            final PathElement element = iterator.next();
            return getOrCreate(element.getKey()).createChild(iterator, element.getValue(), resourceTransformer, defaultTransformer);
        }
    }

    protected void registerTransformer(final Iterator<PathElement> iterator, String operationName, OperationTransformerEntry entry) {
        if(! iterator.hasNext()) {
            final OperationTransformerEntry existing = entriesUpdater.putIfAbsent(this, operationName, entry);
            if(existing != null) {
                throw new IllegalStateException("duplicate transformer " + operationName);
            }
        } else {
            final PathElement element = iterator.next();
            getOrCreate(element.getKey()).registerTransformer(iterator, element.getValue(), operationName, entry);
        }
    }

    protected ResourceTransformerEntry resolveResourceTransformer(final Iterator<PathElement> iterator, final ResourceTransformerEntry inherited) {
        if(! iterator.hasNext()) {
            if(resourceTransformer == null) {
                return inherited;
            }
            return resourceTransformer;
        } else {
            final ResourceTransformerEntry inheritedEntry = resourceTransformer.inherited ? resourceTransformer : inherited;
            final PathElement element = iterator.next();
            final String key = element.getKey();
            SubRegistry registry = subRegistriesUpdater.get(this, key);
            if(registry == null) {
                return inherited;
            }
            return registry.resolveResourceTransformer(iterator, element.getValue(), inheritedEntry);
        }
    }

    protected OperationTransformerEntry resolveTransformer(final Iterator<PathElement> iterator, final String operationName) {
        if(! iterator.hasNext()) {
            final OperationTransformerEntry entry = entriesUpdater.get(this, operationName);
            if(entry == null) {
                return defaultTransformer;
            }
            return entry;
        } else {
            final PathElement element = iterator.next();
            final String key = element.getKey();
            SubRegistry registry = subRegistriesUpdater.get(this, key);
            if(registry == null) {
                return null;
            }
            return registry.resolveTransformer(iterator, element.getValue(), operationName);
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

    static class SubRegistry {

        private static final AtomicMapFieldUpdater<SubRegistry, String, OperationTransformerRegistry> childrenUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(SubRegistry.class, Map.class, "entries"));
        private volatile Map<String, OperationTransformerRegistry> entries;

        SubRegistry() {
            childrenUpdater.clear(this);
        }

        public OperationTransformerEntry resolveTransformer(Iterator<PathElement> iterator, String value, String operationName) {
            final OperationTransformerRegistry reg = get(value);
            if(reg == null) {
                return null;
            }
            return reg.resolveTransformer(iterator, operationName);
        }

        public OperationTransformerRegistry createChild(Iterator<PathElement> iterator, String value, ResourceTransformerEntry resourceTransformer, OperationTransformerEntry defaultTransformer) {
            if(! iterator.hasNext()) {
                return create(value, resourceTransformer, defaultTransformer);
            } else {
                OperationTransformerRegistry entry = get(value);
                if(entry == null) {
                    entry = create(value, GlobalTransformerRegistry.RESOURCE_TRANSFORMER, FORWARD);
                }
                return entry.createChildRegistry(iterator, resourceTransformer, defaultTransformer);
            }
        }

        public void registerTransformer(Iterator<PathElement> iterator, String value, String operationName,  OperationTransformerEntry entry) {
            get(value).registerTransformer(iterator, operationName, entry);
        }

        OperationTransformerRegistry get(final String value) {
            OperationTransformerRegistry entry = childrenUpdater.get(this, value);
            if(entry == null) {
                entry = childrenUpdater.get(this, "*");
                if(entry == null) {
                    return null;
                }
            }
            return entry;
        }

        OperationTransformerRegistry get(final String value, Iterator<PathElement> iterator) {
            OperationTransformerRegistry entry = childrenUpdater.get(this, value);
            if(entry == null) {
                entry = childrenUpdater.get(this, "*");
                if(entry == null) {
                    return null;
                }
            }
            return entry.resolveChild(iterator);
        }

        OperationTransformerRegistry create(final String value, final ResourceTransformerEntry resourceTransformer,final OperationTransformerEntry defaultTransformer) {
            for(;;) {
                final Map<String, OperationTransformerRegistry> entries = childrenUpdater.get(this);
                OperationTransformerRegistry entry = entries.get(value);
                if(entry != null) {
                    return entry;
                } else {
                    entry = new OperationTransformerRegistry(resourceTransformer, defaultTransformer);
                    final OperationTransformerRegistry existing = childrenUpdater.putAtomic(this, value, entry, entries);
                    if(existing == null) {
                        return entry;
                    } else if(existing != entry) {
                        return existing;
                    }
                }
            }
        }

        public ResourceTransformerEntry resolveResourceTransformer(Iterator<PathElement> iterator, String value, ResourceTransformerEntry inheritedEntry) {
            final OperationTransformerRegistry registry = get(value);
            if(registry == null) {
                return inheritedEntry;
            }
            return registry.resolveResourceTransformer(iterator, inheritedEntry);
        }
    }

    public static enum TransformationPolicy {

        // Transform the operation
        TRANSFORM,
        // Forward unmodified
        FORWARD,
        // Discard, don't forward
        DISCARD,
        ;

    }

    public static class ResourceTransformerEntry {

        private final ResourceTransformer transformer;
        private final boolean inherited;

        public ResourceTransformerEntry(ResourceTransformer transformer, boolean inherited) {
            this.transformer = transformer;
            this.inherited = inherited;
        }

        public ResourceTransformer getTransformer() {
            return transformer;
        }

        public boolean isInherited() {
            return inherited;
        }
    }

    public static class OperationTransformerEntry {

        final OperationTransformer transformer;
        final TransformationPolicy policy;

        public OperationTransformerEntry(OperationTransformer transformer, TransformationPolicy policy) {
            this.transformer = transformer;
            this.policy = policy;
        }

        public OperationTransformer getTransformer() {
            return transformer;
        }

        public TransformationPolicy getPolicy() {
            return policy;
        }

    }

    static OperationTransformer FORWARD_TRANSFORMER = new OperationTransformer() {

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) {
            return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }
    };

    static OperationTransformer DISCARD_TRANSFORMER = OperationTransformer.DISCARD;

    public static OperationTransformerEntry DISCARD = new OperationTransformerEntry(DISCARD_TRANSFORMER, TransformationPolicy.DISCARD);
    public static OperationTransformerEntry FORWARD = new OperationTransformerEntry(FORWARD_TRANSFORMER, TransformationPolicy.FORWARD);

}
