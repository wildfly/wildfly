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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResourceDefinition;
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
    private volatile ResourceDefinition resourceDefinition;

    @SuppressWarnings("unused")
    private volatile Map<String, AttributeAccess> attributes;

    private final AtomicBoolean runtimeOnly = new AtomicBoolean();

    private static final AtomicMapFieldUpdater<ConcreteResourceRegistration, String, NodeSubregistry> childrenUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(ConcreteResourceRegistration.class, Map.class, "children"));
    private static final AtomicMapFieldUpdater<ConcreteResourceRegistration, String, OperationEntry> operationsUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(ConcreteResourceRegistration.class, Map.class, "operations"));
    private static final AtomicMapFieldUpdater<ConcreteResourceRegistration, String, AttributeAccess> attributesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(ConcreteResourceRegistration.class, Map.class, "attributes"));
    private static final AtomicReferenceFieldUpdater<ConcreteResourceRegistration, ResourceDefinition> descriptionProviderUpdater = AtomicReferenceFieldUpdater.newUpdater(ConcreteResourceRegistration.class, ResourceDefinition.class, "resourceDefinition");

    ConcreteResourceRegistration(final String valueString, final NodeSubregistry parent, final ResourceDefinition provider, final boolean runtimeOnly) {
        super(valueString, parent);
        childrenUpdater.clear(this);
        operationsUpdater.clear(this);
        attributesUpdater.clear(this);
        descriptionProviderUpdater.set(this, provider);
        this.runtimeOnly.set(runtimeOnly);
    }

    @Override
    public boolean isRuntimeOnly() {
        return runtimeOnly.get();
    }

    @Override
    public void setRuntimeOnly(final boolean runtimeOnly) {
        this.runtimeOnly.set(runtimeOnly);
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public ManagementResourceRegistration registerSubModel(final ResourceDefinition resourceDefinition) {
        if (resourceDefinition == null) {
            throw MESSAGES.nullVar("resourceDefinition");
        }
        final PathElement address = resourceDefinition.getPathElement();
        if (address == null) {
            throw MESSAGES.cannotRegisterSubmodelWithNullPath();
        }
        if (isRuntimeOnly()) {
            throw MESSAGES.cannotRegisterSubmodel();
        }
        final AbstractResourceRegistration existing = getSubRegistration(PathAddress.pathAddress(address));
        if (existing != null && existing.getValueString().equals(address.getValue())) {
            throw MESSAGES.nodeAlreadyRegistered(existing.getLocationString());
        }
        final String key = address.getKey();
        final NodeSubregistry child = getOrCreateSubregistry(key);
        final ManagementResourceRegistration resourceRegistration = child.register(address.getValue(), resourceDefinition, false);
        resourceDefinition.registerAttributes(resourceRegistration);
        resourceDefinition.registerOperations(resourceRegistration);
        return resourceRegistration;
    }

    public void unregisterSubModel(final PathElement address) throws IllegalArgumentException {
        final Map<String, NodeSubregistry> snapshot = childrenUpdater.get(this);
        final NodeSubregistry subregistry = snapshot.get(address.getKey());
        if (subregistry != null) {
            subregistry.unregisterSubModel(address.getValue());
        }
    }

    @Override
    OperationEntry getOperationEntry(final ListIterator<PathElement> iterator, final String operationName, OperationEntry inherited) {
        if (iterator.hasNext()) {
            OperationEntry ourInherited = getInheritableOperationEntry(operationName);
            OperationEntry inheritance = ourInherited == null ? inherited : ourInherited;
            final PathElement next = iterator.next();
            final NodeSubregistry subregistry = children.get(next.getKey());
            if (subregistry == null) {
                return null;
            }
            return subregistry.getOperationEntry(iterator, next.getValue(), operationName, inheritance);
        } else {
            final OperationEntry entry = operationsUpdater.get(this, operationName);
            return entry == null ? inherited : entry;
        }
    }

    @Override
    OperationEntry getInheritableOperationEntry(final String operationName) {
        final OperationEntry entry = operationsUpdater.get(this, operationName);
        if (entry != null && entry.isInherited()) {
            return entry;
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
            throw alreadyRegistered("operation handler", operationName);
        }
    }

    @Override
    public void registerOperationHandler(final String operationName, final OperationStepHandler handler, final DescriptionProvider descriptionProvider, final boolean inherited, EntryType entryType, EnumSet<OperationEntry.Flag> flags) {
        if (operationsUpdater.putIfAbsent(this, operationName, new OperationEntry(handler, descriptionProvider, inherited, entryType, flags)) != null) {
            throw alreadyRegistered("operation handler", operationName);
        }
    }

    @Override
    public void unregisterOperationHandler(final String operationName) {
        if (operationsUpdater.remove(this, operationName) == null) {
            throw operationNotRegisteredException(operationName, resourceDefinition.getPathElement());
        }
    }

    @Override
    public void registerReadWriteAttribute(final String attributeName, final OperationStepHandler readHandler, final OperationStepHandler writeHandler, AttributeAccess.Storage storage) {
        AttributeAccess aa = new AttributeAccess(AccessType.READ_WRITE, storage, readHandler, writeHandler, null, null);
        if (attributesUpdater.putIfAbsent(this, attributeName, aa) != null) {
            throw alreadyRegistered("attribute", attributeName);
        }
    }

    @Override
    public void registerReadWriteAttribute(final String attributeName, final OperationStepHandler readHandler, final OperationStepHandler writeHandler, EnumSet<AttributeAccess.Flag> flags) {
        AttributeAccess.Storage storage = (flags != null && flags.contains(AttributeAccess.Flag.STORAGE_RUNTIME)) ? Storage.RUNTIME : Storage.CONFIGURATION;
        AttributeAccess aa = new AttributeAccess(AccessType.READ_WRITE, storage, readHandler, writeHandler, null, flags);
        if (attributesUpdater.putIfAbsent(this, attributeName, aa) != null) {
            throw alreadyRegistered("attribute", attributeName);
        }
    }

    @Override
    public void registerReadWriteAttribute(final AttributeDefinition definition, final OperationStepHandler readHandler, final OperationStepHandler writeHandler) {
        final EnumSet<AttributeAccess.Flag> flags = definition.getFlags();
        final String attributeName = definition.getName();
        AttributeAccess.Storage storage = (flags != null && flags.contains(AttributeAccess.Flag.STORAGE_RUNTIME)) ? Storage.RUNTIME : Storage.CONFIGURATION;
        AttributeAccess aa = new AttributeAccess(AccessType.READ_WRITE, storage, readHandler, writeHandler, definition, flags);
        if (attributesUpdater.putIfAbsent(this, attributeName, aa) != null) {
            throw alreadyRegistered("attribute", attributeName);
        }
    }

    @Override
    public void registerReadOnlyAttribute(final String attributeName, final OperationStepHandler readHandler, AttributeAccess.Storage storage) {
        AttributeAccess aa = new AttributeAccess(AccessType.READ_ONLY, storage, readHandler, null, null, null);
        if (attributesUpdater.putIfAbsent(this, attributeName, aa) != null) {
            throw alreadyRegistered("attribute", attributeName);
        }
    }

    @Override
    public void registerReadOnlyAttribute(final String attributeName, final OperationStepHandler readHandler, EnumSet<AttributeAccess.Flag> flags) {
        AttributeAccess.Storage storage = (flags != null && flags.contains(AttributeAccess.Flag.STORAGE_RUNTIME)) ? Storage.RUNTIME : Storage.CONFIGURATION;
        AttributeAccess aa = new AttributeAccess(AccessType.READ_ONLY, storage, readHandler, null, null, null);
        if (attributesUpdater.putIfAbsent(this, attributeName, aa) != null) {
            throw alreadyRegistered("attribute", attributeName);
        }
    }

    @Override
    public void registerReadOnlyAttribute(final AttributeDefinition definition, final OperationStepHandler readHandler) {
        final EnumSet<AttributeAccess.Flag> flags = definition.getFlags();
        final String attributeName = definition.getName();
        AttributeAccess.Storage storage = (flags != null && flags.contains(AttributeAccess.Flag.STORAGE_RUNTIME)) ? Storage.RUNTIME : Storage.CONFIGURATION;
        AttributeAccess aa = new AttributeAccess(AccessType.READ_ONLY, storage, readHandler, null, definition, flags);
        if (attributesUpdater.putIfAbsent(this, attributeName, aa) != null) {
            throw alreadyRegistered("attribute", attributeName);
        }
    }

    @Override
    public void registerMetric(String attributeName, OperationStepHandler metricHandler) {
        registerMetric(attributeName, metricHandler, null);
    }

    @Override
    public void registerMetric(String attributeName, OperationStepHandler metricHandler, EnumSet<AttributeAccess.Flag> flags) {
        AttributeAccess aa = new AttributeAccess(AccessType.METRIC, AttributeAccess.Storage.RUNTIME, metricHandler, null, null, flags);
        if (attributesUpdater.putIfAbsent(this, attributeName, aa) != null) {
            throw alreadyRegistered("attribute", attributeName);
        }
    }

    @Override
    public void unregisterAttribute(String attributeName) {
        attributesUpdater.remove(this, attributeName);
    }

    @Override
    public void registerMetric(AttributeDefinition definition, OperationStepHandler metricHandler) {
        AttributeAccess aa = new AttributeAccess(AccessType.METRIC, AttributeAccess.Storage.RUNTIME, metricHandler, null, definition, definition.getFlags());
        if (attributesUpdater.putIfAbsent(this, definition.getName(), aa) != null) {
            throw alreadyRegistered("attribute", definition.getName());
        }
    }

    @Override
    public void registerProxyController(final PathElement address, final ProxyController controller) throws IllegalArgumentException {
        final AbstractResourceRegistration existing = getSubRegistration(PathAddress.pathAddress(address));
        if (existing != null && existing.getValueString().equals(address.getValue())) {
            throw MESSAGES.nodeAlreadyRegistered(existing.getLocationString());
        }
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

    @Override
    DescriptionProvider getModelDescription(final ListIterator<PathElement> iterator) {
        if (iterator.hasNext()) {
            final PathElement next = iterator.next();
            final NodeSubregistry subregistry = children.get(next.getKey());
            if (subregistry == null) {
                return null;
            }
            return subregistry.getModelDescription(iterator, next.getValue());
        } else {
            return resourceDefinition.getDescriptionProvider(this);
        }
    }

    @Override
    Set<String> getAttributeNames(final ListIterator<PathElement> iterator) {
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
            if (access == null && hasNoAlternativeWildcardRegistration()) {
                // If there is metadata for an attribute but no AttributeAccess, assume RO. Can't
                // be writable without a registered handler. This opens the possibility that out-of-date metadata
                // for attribute "foo" can lead to a read of non-existent-in-model "foo" with
                // an unexpected undefined value returned. But it removes the possibility of a
                // dev forgetting to call registry.registerReadOnlyAttribute("foo", null) resulting
                // in the valid attribute "foo" not being readable
                final ModelNode desc = resourceDefinition.getDescriptionProvider(this).getModelDescription(null);
                if (desc.has(ATTRIBUTES) && desc.get(ATTRIBUTES).keys().contains(attributeName)) {
                    access = new AttributeAccess(AccessType.READ_ONLY, Storage.CONFIGURATION, null, null, null, null);
                }
            }
            return access;
        }
    }

    @Override
    Set<String> getChildNames(final ListIterator<PathElement> iterator) {
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
    Set<PathElement> getChildAddresses(final ListIterator<PathElement> iterator) {
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
    ProxyController getProxyController(ListIterator<PathElement> iterator) {
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
    void getProxyControllers(ListIterator<PathElement> iterator, Set<ProxyController> controllers) {
        if (iterator.hasNext()) {
            final PathElement next = iterator.next();
            final NodeSubregistry subregistry = children.get(next.getKey());
            if (subregistry == null) {
                return;
            }
            if (next.isWildcard()) {
                subregistry.getProxyControllers(iterator, null, controllers);
            } else if (next.isMultiTarget()) {
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

    @Override
    AbstractResourceRegistration getResourceRegistration(ListIterator<PathElement> iterator) {
        if (! iterator.hasNext()) {
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

    private IllegalArgumentException alreadyRegistered(final String type, final String name) {
        return MESSAGES.alreadyRegistered(type, name, getLocationString());
    }

    private IllegalArgumentException operationNotRegisteredException(String op, PathElement address) {
        return MESSAGES.operationNotRegisteredException(op, PathAddress.pathAddress(address));
    }

}

