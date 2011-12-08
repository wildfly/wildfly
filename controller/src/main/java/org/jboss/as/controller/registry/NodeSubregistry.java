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
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;

/**
 * A registry of values within a specific key type.
 */
final class NodeSubregistry {

    private static final String WILDCARD_VALUE = PathElement.WILDCARD_VALUE;

    private final String keyName;
    private final ConcreteResourceRegistration parent;
    @SuppressWarnings( { "unused" })
    private volatile Map<String, AbstractResourceRegistration> childRegistries;

    private static final AtomicMapFieldUpdater<NodeSubregistry, String, AbstractResourceRegistration> childRegistriesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(NodeSubregistry.class, Map.class, "childRegistries"));

    NodeSubregistry(final String keyName, final ConcreteResourceRegistration parent) {
        this.keyName = keyName;
        this.parent = parent;
        childRegistriesUpdater.clear(this);
    }

    AbstractResourceRegistration getParent() {
        return parent;
    }

    Set<String> getChildNames(){
        final Map<String, AbstractResourceRegistration> snapshot = this.childRegistries;
        if (snapshot == null) {
            return Collections.emptySet();
        }
        return new HashSet<String>(snapshot.keySet());
    }

    ManagementResourceRegistration register(final String elementValue, final ResourceDefinition provider, boolean runtimeOnly) {
        final AbstractResourceRegistration newRegistry = new ConcreteResourceRegistration(elementValue, this, provider, runtimeOnly);
        final AbstractResourceRegistration existingRegistry = childRegistriesUpdater.putIfAbsent(this, elementValue, newRegistry);
        if (existingRegistry != null) {
            throw MESSAGES.nodeAlreadyRegistered(getLocationString(), elementValue);
        }
        return newRegistry;
    }

    ProxyControllerRegistration registerProxyController(final String elementValue, final ProxyController proxyController) {
        final ProxyControllerRegistration newRegistry = new ProxyControllerRegistration(elementValue, this, proxyController);
        final AbstractResourceRegistration appearingRegistry = childRegistriesUpdater.putIfAbsent(this, elementValue, newRegistry);
        if (appearingRegistry != null) {
            throw MESSAGES.nodeAlreadyRegistered(getLocationString(), elementValue);
        }
        //register(elementValue, newRegistry);
        return newRegistry;
    }

    void unregisterProxyController(final String elementValue) {
        childRegistriesUpdater.remove(this, elementValue);
    }

    void unregisterSubModel(final String elementValue) {
        childRegistriesUpdater.remove(this, elementValue);
    }

    OperationEntry getOperationEntry(final ListIterator<PathElement> iterator, final String child, final String operationName, OperationEntry inherited) {

        final RegistrySearchControl searchControl = new RegistrySearchControl(iterator, child);

        // First search the non-wildcard child; if not found, search the wildcard child
        OperationEntry result = null;

        if (searchControl.getSpecifiedRegistry() != null) {
            result = searchControl.getSpecifiedRegistry().getOperationEntry(searchControl.getIterator(), operationName, inherited);
        }

        if (result == null && searchControl.getWildCardRegistry() != null) {
            result = searchControl.getWildCardRegistry().getOperationEntry(searchControl.getIterator(), operationName, inherited);
        }

        return result;
    }

    void getHandlers(final ListIterator<PathElement> iterator, final String child, final Map<String, OperationEntry> providers, final boolean inherited) {

        final RegistrySearchControl searchControl = new RegistrySearchControl(iterator, child);

        // First search the wildcard child, then if there is a non-wildcard child search it
        // Non-wildcard goes second so its description overwrites in case of duplicates

        if (searchControl.getWildCardRegistry() != null) {
            searchControl.getWildCardRegistry().getOperationDescriptions(searchControl.getIterator(), providers, inherited);
        }

        if (searchControl.getSpecifiedRegistry() != null) {
            searchControl.getSpecifiedRegistry().getOperationDescriptions(searchControl.getIterator(), providers, inherited);
        }
    }

    String getLocationString() {
        return parent.getLocationString() + "(" + keyName + " => ";
    }

    DescriptionProvider getModelDescription(final ListIterator<PathElement> iterator, final String child) {

        final RegistrySearchControl searchControl = new RegistrySearchControl(iterator, child);

        // First search the non-wildcard child; if not found, search the wildcard child
        DescriptionProvider result = null;

        if (searchControl.getSpecifiedRegistry() != null) {
            result = searchControl.getSpecifiedRegistry().getModelDescription(searchControl.getIterator());
        }

        if (result == null && searchControl.getWildCardRegistry() != null) {
            result = searchControl.getWildCardRegistry().getModelDescription(searchControl.getIterator());
        }

        return result;
    }

    Set<String> getChildNames(final ListIterator<PathElement> iterator, final String child){

        final RegistrySearchControl searchControl = new RegistrySearchControl(iterator, child);

        Set<String> result = null;
        if (searchControl.getSpecifiedRegistry() != null) {
            result = searchControl.getSpecifiedRegistry().getChildNames(searchControl.getIterator());
        }

        if (searchControl.getWildCardRegistry() != null) {
            final Set<String> wildCardChildren = searchControl.getWildCardRegistry().getChildNames(searchControl.getIterator());
            if (result == null) {
                result = wildCardChildren;
            } else if (wildCardChildren != null) {
                // Merge
                result = new HashSet<String>(result);
                result.addAll(wildCardChildren);
            }
        }
        return result;
    }

    Set<String> getAttributeNames(final ListIterator<PathElement> iterator, final String child){

        final RegistrySearchControl searchControl = new RegistrySearchControl(iterator, child);

        Set<String> result = null;
        if (searchControl.getSpecifiedRegistry() != null) {
            result = searchControl.getSpecifiedRegistry().getAttributeNames(searchControl.getIterator());
        }

        if (searchControl.getWildCardRegistry() != null) {
            final Set<String> wildCardChildren = searchControl.getWildCardRegistry().getAttributeNames(searchControl.getIterator());
            if (result == null) {
                result = wildCardChildren;
            } else if (wildCardChildren != null) {
                // Merge
                result = new HashSet<String>(result);
                result.addAll(wildCardChildren);
            }
        }
        return result;
    }

    AttributeAccess getAttributeAccess(final ListIterator<PathElement> iterator, final String child, final String attributeName) {

        final RegistrySearchControl searchControl = new RegistrySearchControl(iterator, child);

        // First search the non-wildcard child; if not found, search the wildcard child
        AttributeAccess result = null;

        if (searchControl.getSpecifiedRegistry() != null) {
            result = searchControl.getSpecifiedRegistry().getAttributeAccess(searchControl.getIterator(), attributeName);
        }

        if (result == null && searchControl.getWildCardRegistry() != null) {
            result = searchControl.getWildCardRegistry().getAttributeAccess(searchControl.getIterator(), attributeName);
        }

        return result;
    }


    Set<PathElement> getChildAddresses(final ListIterator<PathElement> iterator, final String child){

        final RegistrySearchControl searchControl = new RegistrySearchControl(iterator, child);

        Set<PathElement> result = null;
        if (searchControl.getSpecifiedRegistry() != null) {
            result = searchControl.getSpecifiedRegistry().getChildAddresses(searchControl.getIterator());
        }

        if (searchControl.getWildCardRegistry() != null) {
            final Set<PathElement> wildCardChildren = searchControl.getWildCardRegistry().getChildAddresses(searchControl.getIterator());
            if (result == null) {
                result = wildCardChildren;
            } else if (wildCardChildren != null) {
                // Merge
                result = new HashSet<PathElement>(result);
                result.addAll(wildCardChildren);
            }
        }
        return result;
    }

    ProxyController getProxyController(final ListIterator<PathElement> iterator, final String child) {

        final RegistrySearchControl searchControl = new RegistrySearchControl(iterator, child);

        // First search the non-wildcard child; if not found, search the wildcard child
        ProxyController result = null;

        if (searchControl.getSpecifiedRegistry() != null) {
            result = searchControl.getSpecifiedRegistry().getProxyController(searchControl.getIterator());
        }

        if (result == null && searchControl.getWildCardRegistry() != null) {
            result = searchControl.getWildCardRegistry().getProxyController(searchControl.getIterator());
        }

        return result;
    }

    AbstractResourceRegistration getResourceRegistration(final ListIterator<PathElement> iterator, final String child) {

        final RegistrySearchControl searchControl = new RegistrySearchControl(iterator, child);

        // First search the non-wildcard child; if not found, search the wildcard child
        AbstractResourceRegistration result = null;

        if (searchControl.getSpecifiedRegistry() != null) {
            result = searchControl.getSpecifiedRegistry().getResourceRegistration(searchControl.getIterator());
        }

        if (result == null && searchControl.getWildCardRegistry() != null) {
            result = searchControl.getWildCardRegistry().getResourceRegistration(searchControl.getIterator());
        }

        return result;
    }

    void getProxyControllers(final ListIterator<PathElement> iterator, final String child, Set<ProxyController> controllers) {
        if (child != null) {
            final RegistrySearchControl searchControl = new RegistrySearchControl(iterator, child);

            // First search the wildcard child, then if there is a non-wildcard child search it

            if (searchControl.getWildCardRegistry() != null) {
                searchControl.getWildCardRegistry().getProxyControllers(searchControl.getIterator(), controllers);
            }

            if (searchControl.getSpecifiedRegistry() != null) {
                searchControl.getSpecifiedRegistry().getProxyControllers(searchControl.getIterator(), controllers);
            }
        } else {
            final Map<String, AbstractResourceRegistration> snapshot = childRegistriesUpdater.get(NodeSubregistry.this);
            for (AbstractResourceRegistration childRegistry : snapshot.values()) {
                childRegistry.getProxyControllers(iterator, controllers);
            }
        }
    }

    /**
     * Encapsulates data and behavior to help with searches in both a specified child and in the wildcard child if
     * it exists and is different from the specified child
     */
    private class RegistrySearchControl {
        private final AbstractResourceRegistration specifiedRegistry;
        private final AbstractResourceRegistration wildCardRegistry;
        private final ListIterator<PathElement> iterator;
        private final int restoreIndex;
        private boolean backupRequired;

        private RegistrySearchControl(final ListIterator<PathElement> iterator, final String childName) {
            final Map<String, AbstractResourceRegistration> snapshot = childRegistriesUpdater.get(NodeSubregistry.this);
            this.specifiedRegistry = snapshot.get(childName);
            this.wildCardRegistry = WILDCARD_VALUE.equals(childName) ? null : snapshot.get(WILDCARD_VALUE);
            this.iterator = iterator;
            this.restoreIndex = (specifiedRegistry != null && wildCardRegistry != null) ? iterator.nextIndex() : -1;
        }

        private AbstractResourceRegistration getSpecifiedRegistry() {
            return specifiedRegistry;
        }

        private AbstractResourceRegistration getWildCardRegistry() {
            return wildCardRegistry;
        }

        private ListIterator<PathElement> getIterator() {
            if (backupRequired) {
                if (restoreIndex == -1) {
                    // Coding mistake; someone wants to search twice for no reason, since we only have a single registration
                    throw new IllegalStateException("Multiple iterator requests are not supported since both " +
                            "named and wildcard entries were not present");
                }
                // Back the iterator to the restore index
                while (iterator.nextIndex() > restoreIndex) {
                    iterator.previous();
                }
            }
            backupRequired = true;
            return iterator;
        }
    }

    String getKeyName() {
        return keyName;
    }
}
