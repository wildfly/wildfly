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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Resolved operation transformer registry.
 *
 * @author Emanuel Muckenhuber
 */
public class OperationTransformerRegistry {

    private volatile Map<String, SubRegistry> subRegistries;
    private volatile Map<String, TransformerEntry> transformerEntries;

    private static final AtomicMapFieldUpdater<OperationTransformerRegistry, String, SubRegistry> subRegistriesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(OperationTransformerRegistry.class, Map.class, "subRegistries"));
    private static final AtomicMapFieldUpdater<OperationTransformerRegistry, String, TransformerEntry> entriesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(OperationTransformerRegistry.class, Map.class, "transformerEntries"));

    public OperationTransformerRegistry() {
        entriesUpdater.clear(this);
        subRegistriesUpdater.clear(this);
    }

    /**
     * Resolve an operation transformer entry.
     *
     * @param address the address
     * @param operationName the operation name
     * @return the transformer entry
     */
    public TransformerEntry resolveTransformer(final PathAddress address, final String operationName) {
        final Iterator<PathElement> iterator = address.iterator();
        final TransformerEntry entry = resolveTransformer(iterator, operationName);
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
    public void mergeSubsystem(final GlobalOperationTransformerRegistry registry, String subsystemName, ModelVersion version) {
        final SubRegistry subRegistry = getOrCreate(SUBSYSTEM);
        final OperationTransformerRegistry subsystemReg = subRegistry.getOrCreate(subsystemName);
        final PathElement element = PathElement.pathElement(SUBSYSTEM, subsystemName);
        registry.mergeSubtree(this, PathAddress.EMPTY_ADDRESS.append(element), version);
    }

    protected void registerTransformer(final PathAddress address, final String operationName, final OperationTransformer transformer) {
        registerTransformer(address.iterator(), operationName, new TransformerEntry(transformer, TransformationPolicy.TRANSFORM));
    }

    protected Map<String, TransformerEntry> getTransformers() {
        return entriesUpdater.get(this);
    }

    protected void registerTransformer(final Iterator<PathElement> iterator, String operationName, TransformerEntry entry) {
        if(! iterator.hasNext()) {
            final TransformerEntry existing = entriesUpdater.putIfAbsent(this, operationName, entry);
            if(existing != null) {
                throw new IllegalStateException("duplicate transformer " + operationName);
            }
        } else {
            final PathElement element = iterator.next();
            getOrCreate(element.getKey()).registerTransformer(iterator, element.getValue(), operationName, entry);
        }
    }

    protected TransformerEntry resolveTransformer(final Iterator<PathElement> iterator, final String operationName) {
        if(! iterator.hasNext()) {
            return entriesUpdater.get(this, operationName);
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

        public TransformerEntry resolveTransformer(Iterator<PathElement> iterator, String value, String operationName) {
            OperationTransformerRegistry entry = childrenUpdater.get(this, value);
            if(entry == null) {
                entry = childrenUpdater.get(this, "*");
                if(entry == null) {
                    return null;
                }
            }
            return entry.resolveTransformer(iterator, operationName);
        }

        public void registerTransformer(Iterator<PathElement> iterator, String value, String operationName, TransformerEntry entry) {
            getOrCreate(value).registerTransformer(iterator, operationName, entry);
        }

        OperationTransformerRegistry getOrCreate(final String value) {
            for(;;) {
                final Map<String, OperationTransformerRegistry> entries = childrenUpdater.get(this);
                OperationTransformerRegistry entry = entries.get(value);
                if(entry != null) {
                    return entry;
                } else {
                    entry = new OperationTransformerRegistry();
                    final OperationTransformerRegistry existing = childrenUpdater.putAtomic(this, value, entry, entries);
                    if(existing == null) {
                        return entry;
                    } else if(existing != entry) {
                        return existing;
                    }
                }
            }
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

    public static class TransformerEntry {

        final OperationTransformer transformer;
        final TransformationPolicy policy;

        public TransformerEntry(OperationTransformer transformer, TransformationPolicy policy) {
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

    static OperationTransformer DISCARD_TRANSFORMER = new OperationTransformer() {

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) {
            // hmm...
            return new TransformedOperation(null, new OperationResultTransformer() {
                            @Override
                            public ModelNode transformResult(ModelNode ignore) {
                                final ModelNode result = new ModelNode();
                                result.get(OUTCOME).set(SUCCESS);
                                result.get(RESULT);
                                // perhaps some other param indicating that the operation was ignored
                                return result;
                            }
            });
        }
    };

    static TransformerEntry DISCARD = new TransformerEntry(DISCARD_TRANSFORMER, TransformationPolicy.DISCARD);
    static TransformerEntry FORWARD = new TransformerEntry(FORWARD_TRANSFORMER, TransformationPolicy.FORWARD);

}
