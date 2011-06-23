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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.AttributeAccess.AccessType;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.dmr.ModelNode;

final class ConcreteResourceRegistration extends AbstractResourceRegistration {

    @SuppressWarnings("unused")
    private volatile Map<String, NodeSubregistry> children;

    @SuppressWarnings("unused")
    private volatile Map<String, OperationEntry> operations;

    @SuppressWarnings("unused")
    private volatile DescriptionProvider descriptionProvider;

    @SuppressWarnings("unused")
    private volatile Map<String, AttributeAccess> attributes;

    private final boolean runtimeOnly;

    private static final AtomicMapFieldUpdater<ConcreteResourceRegistration, String, NodeSubregistry> childrenUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(ConcreteResourceRegistration.class, Map.class, "children"));
    private static final AtomicMapFieldUpdater<ConcreteResourceRegistration, String, OperationEntry> operationsUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(ConcreteResourceRegistration.class, Map.class, "operations"));
    private static final AtomicMapFieldUpdater<ConcreteResourceRegistration, String, AttributeAccess> attributesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(ConcreteResourceRegistration.class, Map.class, "attributes"));
    private static final AtomicReferenceFieldUpdater<ConcreteResourceRegistration, DescriptionProvider> descriptionProviderUpdater = AtomicReferenceFieldUpdater.newUpdater(ConcreteResourceRegistration.class, DescriptionProvider.class, "descriptionProvider");

    ConcreteResourceRegistration(final String valueString, final NodeSubregistry parent, final DescriptionProvider provider, final boolean runtimeOnly) {
        super(valueString, parent);
        childrenUpdater.clear(this);
        operationsUpdater.clear(this);
        attributesUpdater.clear(this);
        descriptionProviderUpdater.set(this, provider);
        this.runtimeOnly = runtimeOnly;
    }

    @Override
    public boolean isRuntimeOnly() {
        return runtimeOnly;
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public ManagementResourceRegistration registerSubModel(final PathElement address, final DescriptionProvider descriptionProvider) {
        if (address == null) {
            throw new IllegalArgumentException("address is null");
        }
        if (descriptionProvider == null) {
            throw new IllegalArgumentException("descriptionProvider is null");
        }
        if (runtimeOnly) {
            throw new IllegalStateException("Cannot register non-runtime-only submodels with a runtime-only parent");
        }
        final String key = address.getKey();
        final NodeSubregistry child = getOrCreateSubregistry(key);
        return child.register(address.getValue(), descriptionProvider, false);
    }

    @Override
    public void registerSubModel(final PathElement address, final ManagementResourceRegistration subModel) {
        if (address == null) {
            throw new IllegalArgumentException("address is null");
        }
        if (subModel == null) {
            throw new IllegalArgumentException("subModel is null");
        }
        final String key = address.getKey();
        final NodeSubregistry child = getOrCreateSubregistry(key);
        child.register(address.getValue(), subModel);
    }

    @Override
    OperationStepHandler getOperationHandler(final ListIterator<PathElement> iterator, final String operationName, OperationStepHandler inherited) {
        if (iterator.hasNext()) {
            OperationStepHandler ourInherited = getInheritableOperationHandler(operationName);
            OperationStepHandler inheritance = ourInherited == null ? inherited : ourInherited;
            final PathElement next = iterator.next();
            final NodeSubregistry subregistry = children.get(next.getKey());
            if (subregistry == null) {
                return null;
            }
            return subregistry.getOperationHandler(iterator, next.getValue(), operationName, inheritance);
        } else {
            final OperationEntry entry = operationsUpdater.get(this, operationName);
            return entry == null ? inherited : entry.getOperationHandler();
        }
    }

    @Override
    OperationStepHandler getInheritableOperationHandler(final String operationName) {
        final OperationEntry entry = operationsUpdater.get(this, operationName);
        if (entry != null && entry.isInherited()) {
            return entry.getOperationHandler();
        }
        return null;
    }

    @Override
    void getOperationDescriptions(final ListIterator<PathElement> iterator, final Map<String, OperationEntry> providers, final boolean inherited) {

        if (!iterator.hasNext() ) {
            providers.putAll(operationsUpdater.get(this));
            if (inherited) {
                getInheritedOperations(providers, true);
            }
            return;
        }
        final PathElement next = iterator.next();
        try {
            final String key = next.getKey();
            final Map<String, NodeSubregistry> snapshot = childrenUpdater.get(this);
            final NodeSubregistry subregistry = snapshot.get(key);
            if (subregistry != null) {
                subregistry.getHandlers(iterator, next.getValue(), providers, inherited);
            }
        } finally {
            iterator.previous();
        }
    }

    @Override
    void getInheritedOperationEntries(final Map<String, OperationEntry> providers) {
        for (final Map.Entry<String, OperationEntry> entry : operationsUpdater.get(this).entrySet()) {
            if (entry.getValue().isInherited() && !providers.containsKey(entry.getKey())) {
                providers.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void registerOperationHandler(final String operationName, final OperationStepHandler handler, final DescriptionProvider descriptionProvider, final boolean inherited, EntryType entryType) {
        if (operationsUpdater.putIfAbsent(this, operationName, new OperationEntry(handler, descriptionProvider, inherited, entryType)) != null) {
            throw new IllegalArgumentException("A handler named '" + operationName + "' is already registered at location '" + getLocationString() + "'");
        }
    }

    @Override
    public void registerOperationHandler(final String operationName, final OperationStepHandler handler, final DescriptionProvider descriptionProvider, final boolean inherited, EntryType entryType, EnumSet<OperationEntry.Flag> flags) {
        if (operationsUpdater.putIfAbsent(this, operationName, new OperationEntry(handler, descriptionProvider, inherited, entryType, flags)) != null) {
            throw new IllegalArgumentException("A handler named '" + operationName + "' is already registered at location '" + getLocationString() + "'");
        }
    }

    @Override
    public void registerReadWriteAttribute(final String attributeName, final OperationStepHandler readHandler, final OperationStepHandler writeHandler, AttributeAccess.Storage storage) {
        if (attributesUpdater.putIfAbsent(this, attributeName, new AttributeAccess(AccessType.READ_WRITE, storage, readHandler, writeHandler)) != null) {
            throw new IllegalArgumentException("An attribute named '" + attributeName + "' is already registered at location '" + getLocationString() + "'");
        }
    }

    @Override
    public void registerReadOnlyAttribute(final String attributeName, final OperationStepHandler readHandler, AttributeAccess.Storage storage) {
        if (attributesUpdater.putIfAbsent(this, attributeName, new AttributeAccess(AccessType.READ_ONLY, storage, readHandler, null)) != null) {
            throw new IllegalArgumentException("An attribute named '" + attributeName + "' is already registered at location '" + getLocationString() + "'");
        }
    }

    @Override
    public void registerMetric(String attributeName, OperationStepHandler metricHandler) {
        if (attributesUpdater.putIfAbsent(this, attributeName, new AttributeAccess(AccessType.METRIC, AttributeAccess.Storage.RUNTIME, metricHandler, null)) != null) {
            throw new IllegalArgumentException("An attribute named '" + attributeName + "' is already registered at location '" + getLocationString() + "'");
        }
    }

    @Override
    public void registerProxyController(final PathElement address, final ProxyController controller) throws IllegalArgumentException {
        getOrCreateSubregistry(address.getKey()).registerProxyController(address.getValue(), controller);
    }

    @Override
    public void unregisterProxyController(final PathElement address) throws IllegalArgumentException {
        final Map<String, NodeSubregistry> snapshot = childrenUpdater.get(this);
        final NodeSubregistry subregistry = snapshot.get(address.getKey());
        if (subregistry != null) {
            subregistry.unregisterProxyController(address.getValue());
        }
    }

    NodeSubregistry getOrCreateSubregistry(final String key) {
        for (;;) {
            final Map<String, NodeSubregistry> snapshot = childrenUpdater.get(this);
            final NodeSubregistry subregistry = snapshot.get(key);
            if (subregistry != null) {
                return subregistry;
            } else {
                final NodeSubregistry newRegistry = new NodeSubregistry(key, this);
                final NodeSubregistry appearing = childrenUpdater.putAtomic(this, key, newRegistry, snapshot);
                if (appearing == null) {
                    return newRegistry;
                } else if (appearing != newRegistry) {
                    // someone else added one
                    return appearing;
                }
                // otherwise, retry the loop because the map changed
            }
        }
    }

//    void registerProxyController(final ListIterator<PathElement> iterator, final ProxyController controller) {
//        if (! iterator.hasNext()) {
//            throw new IllegalArgumentException("Handlers have already been registered at location '" + getLocationString() + "'");
//        }
//        final PathElement element = iterator.next();
//        getOrCreateSubregistry(element.getKey()).registerProxyController(element.getValue(), controller);
//    }

    @Override
    DescriptionProvider getOperationDescription(final Iterator<PathElement> iterator, final String operationName, DescriptionProvider inherited) {
        if (iterator.hasNext()) {
            DescriptionProvider ourInherited = getInheritableOperationDescription(operationName);
            DescriptionProvider inheritance = ourInherited == null ? inherited : ourInherited;
            final PathElement next = iterator.next();
            final NodeSubregistry subregistry = children.get(next.getKey());
            if (subregistry == null) {
                return null;
            }
            return subregistry.getOperationDescription(iterator, next.getValue(), operationName, inheritance);
        } else {
            final OperationEntry entry = operationsUpdater.get(this, operationName);
            return entry == null ? null : entry.getDescriptionProvider();
        }
    }

    @Override
    DescriptionProvider getInheritableOperationDescription(final String operationName) {
        final OperationEntry entry = operationsUpdater.get(this, operationName);
        if (entry != null && entry.isInherited()) {
            return entry.getDescriptionProvider();
        }
        return null;
    }

    @Override
    Set<OperationEntry.Flag> getOperationFlags(ListIterator<PathElement> iterator, String operationName, Set<OperationEntry.Flag> inherited) {
        if (iterator.hasNext()) {
            Set<OperationEntry.Flag> ourInherited = getInheritableOperationFlags(operationName);
            Set<OperationEntry.Flag> inheritance = ourInherited == null ? inherited : ourInherited;
            final PathElement next = iterator.next();
            final NodeSubregistry subregistry = children.get(next.getKey());
            if (subregistry == null) {
                return null;
            }
            return subregistry.getOperationFlags(iterator, next.getValue(), operationName, inheritance);
        } else {
            final OperationEntry entry = operationsUpdater.get(this, operationName);
            return entry == null ? inherited : entry.getFlags();
        }
    }

    @Override
    Set<OperationEntry.Flag> getInheritableOperationFlags(String operationName) {
        final OperationEntry entry = operationsUpdater.get(this, operationName);
        if (entry != null && entry.isInherited()) {
            return entry.getFlags();
        }
        return null;
    }

    @Override
    DescriptionProvider getModelDescription(final Iterator<PathElement> iterator) {
        if (iterator.hasNext()) {
            final PathElement next = iterator.next();
            final NodeSubregistry subregistry = children.get(next.getKey());
            if (subregistry == null) {
                return null;
            }
            return subregistry.getModelDescription(iterator, next.getValue());
        } else {
            return descriptionProvider;
        }
    }

    @Override
    Set<String> getAttributeNames(final Iterator<PathElement> iterator) {
        if (iterator.hasNext()) {
            final PathElement next = iterator.next();
            final NodeSubregistry subregistry = children.get(next.getKey());
            if (subregistry == null) {
                return Collections.emptySet();
            }
            return subregistry.getAttributeNames(iterator, next.getValue());
        } else {
            final Map<String, AttributeAccess> snapshot = attributesUpdater.get(this);
            return snapshot.keySet();
        }
    }

    @Override
    AttributeAccess getAttributeAccess(final ListIterator<PathElement> iterator, final String attributeName) {

        if (iterator.hasNext()) {
            final PathElement next = iterator.next();
            final NodeSubregistry subregistry = children.get(next.getKey());
            if (subregistry == null) {
                return null;
            }
            return subregistry.getAttributeAccess(iterator, next.getValue(), attributeName);
        } else {
            final Map<String, AttributeAccess> snapshot = attributesUpdater.get(this);
            AttributeAccess access = snapshot.get(attributeName);
            if (access == null) {
                // If there is metadata for an attribute but no AttributeAccess, assume RO. Can't
                // be writable without a registered handler. This opens the possibility that out-of-date metadata
                // for attribute "foo" can lead to a read of non-existent-in-model "foo" with
                // an unexpected undefined value returned. But it removes the possibility of a
                // dev forgetting to call registry.registerReadOnlyAttribute("foo", null) resulting
                // in the valid attribute "foo" not being readable
                final ModelNode desc = descriptionProvider.getModelDescription(null);
                if (desc.has(ATTRIBUTES) && desc.get(ATTRIBUTES).keys().contains(attributeName)) {
                    access = new AttributeAccess(AccessType.READ_ONLY, Storage.CONFIGURATION, null, null);
                }
            }
            return access;
        }
    }

    @Override
    Set<String> getChildNames(final Iterator<PathElement> iterator) {
        if (iterator.hasNext()) {
            final PathElement next = iterator.next();
            final NodeSubregistry subregistry = children.get(next.getKey());
            if (subregistry == null) {
                return Collections.emptySet();
            }
            return subregistry.getChildNames(iterator, next.getValue());
        } else {
            final Map<String, NodeSubregistry> children = this.children;
            if (children != null) {
                return Collections.unmodifiableSet(children.keySet());
            }
            return Collections.emptySet();
        }
    }

    @Override
    Set<PathElement> getChildAddresses(final Iterator<PathElement> iterator) {
        if (iterator.hasNext()) {
            final PathElement next = iterator.next();
            final NodeSubregistry subregistry = children.get(next.getKey());
            if (subregistry == null) {
                return Collections.emptySet();
            }
            return subregistry.getChildAddresses(iterator, next.getValue());
        } else {
            final Map<String, NodeSubregistry> children = this.children;
            if (children != null) {
                final Set<PathElement> elements = new HashSet<PathElement>();
                for (final Map.Entry<String, NodeSubregistry> entry : children.entrySet()) {
                    for (final String entryChild : entry.getValue().getChildNames()) {
                        elements.add(PathElement.pathElement(entry.getKey(), entryChild));
                    }
                }
                return elements;
            }
            return Collections.emptySet();
        }
    }

    @Override
    ProxyController getProxyController(Iterator<PathElement> iterator) {
        if (iterator.hasNext()) {
            final PathElement next = iterator.next();
            final NodeSubregistry subregistry = children.get(next.getKey());
            if (subregistry == null) {
                return null;
            }
            return subregistry.getProxyController(iterator, next.getValue());
        } else {
            return null;
        }
    }

    @Override
    void getProxyControllers(Iterator<PathElement> iterator, Set<ProxyController> controllers) {
        if (iterator.hasNext()) {
            final PathElement next = iterator.next();
            final NodeSubregistry subregistry = children.get(next.getKey());
            if (subregistry == null) {
                return;
            }
            if(next.isWildcard()) {
                subregistry.getProxyControllers(iterator, null, controllers);
            } else if(next.isMultiTarget()) {
                for(final String value : next.getSegments()) {
                    subregistry.getProxyControllers(iterator, value, controllers);
                }
            } else {
                subregistry.getProxyControllers(iterator, next.getValue(), controllers);
            }
        } else {
            final Map<String, NodeSubregistry> snapshot = childrenUpdater.get(this);
            for (NodeSubregistry subregistry : snapshot.values()) {
                subregistry.getProxyControllers(iterator, null, controllers);
            }
        }
    }

    ManagementResourceRegistration getResourceRegistration(Iterator<PathElement> iterator) {
        if(! iterator.hasNext()) {
            return this;
        } else {
            final PathElement address = iterator.next();
            final Map<String, NodeSubregistry> snapshot = childrenUpdater.get(this);
            final NodeSubregistry subregistry = snapshot.get(address.getKey());
            if (subregistry != null) {
                return subregistry.getResourceRegistration(iterator, address.getValue());
            } else {
                return null;
            }
        }
    }


}

