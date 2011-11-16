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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

/**
 * A registry of values within a specific key type.
 */
final class NodeSubregistry {

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
        register(elementValue, newRegistry);
        return newRegistry;
    }

    void register(final String elementValue, final ManagementResourceRegistration subModel) {
        AbstractResourceRegistration newRegistry = null;
        if (subModel instanceof AbstractResourceRegistration) {
            newRegistry = (AbstractResourceRegistration) subModel;
        }
        else {

        }
        final AbstractResourceRegistration appearingRegistry = childRegistriesUpdater.putIfAbsent(this, elementValue, newRegistry);
        if (appearingRegistry != null) {
            throw MESSAGES.nodeAlreadyRegistered(getLocationString(), elementValue);
        }
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

    OperationEntry getOperationEntry(final ListIterator<PathElement> iterator, final String child, final String operationName, OperationEntry inherited) {
        final Map<String, AbstractResourceRegistration> snapshot = childRegistriesUpdater.get(this);
        final AbstractResourceRegistration childRegistry = snapshot.get(child);
        if (childRegistry != null) {
            return childRegistry.getOperationEntry(iterator, operationName, inherited);
        } else {
            final AbstractResourceRegistration wildcardRegistry = snapshot.get("*");
            if (wildcardRegistry != null) {
                return wildcardRegistry.getOperationEntry(iterator, operationName, inherited);
            } else {
                return null;
            }
        }
    }

    void getHandlers(final ListIterator<PathElement> iterator, final String child, final Map<String, OperationEntry> providers, final boolean inherited) {
        final Map<String, AbstractResourceRegistration> snapshot = childRegistriesUpdater.get(this);
        final AbstractResourceRegistration childRegistry = snapshot.get(child);
        final AbstractResourceRegistration wildcardRegistry = snapshot.get("*");
        if (wildcardRegistry == null) {
            if (childRegistry != null) {
                childRegistry.getOperationDescriptions(iterator, providers, inherited);
            }
        } else {
            if (childRegistry == null) {
                wildcardRegistry.getOperationDescriptions(iterator, providers, inherited);
            } else {
                wildcardRegistry.getOperationDescriptions(iterator, providers, inherited);
                childRegistry.getOperationDescriptions(iterator, providers, inherited);
            }
        }
    }

    String getLocationString() {
        return parent.getLocationString() + "(" + keyName + " => ";
    }

    DescriptionProvider getModelDescription(final Iterator<PathElement> iterator, final String child) {
        final Map<String, AbstractResourceRegistration> snapshot = childRegistries;
        AbstractResourceRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getModelDescription(iterator);
    }

    Set<String> getChildNames(final Iterator<PathElement> iterator, final String child){
        final Map<String, AbstractResourceRegistration> snapshot = childRegistries;
        AbstractResourceRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getChildNames(iterator);
    }

    Set<String> getAttributeNames(final Iterator<PathElement> iterator, final String child){
        final Map<String, AbstractResourceRegistration> snapshot = childRegistries;
        AbstractResourceRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getAttributeNames(iterator);
    }

    AttributeAccess getAttributeAccess(final ListIterator<PathElement> iterator, final String child, final String attributeName) {
        final Map<String, AbstractResourceRegistration> snapshot = childRegistriesUpdater.get(this);
        final AbstractResourceRegistration childRegistry = snapshot.get(child);
        if (childRegistry != null) {
            return childRegistry.getAttributeAccess(iterator, attributeName);
        } else {
            final AbstractResourceRegistration wildcardRegistry = snapshot.get("*");
            if (wildcardRegistry != null) {
                return wildcardRegistry.getAttributeAccess(iterator, attributeName);
            } else {
                return null;
            }
        }
    }


    Set<PathElement> getChildAddresses(final Iterator<PathElement> iterator, final String child){
        final Map<String, AbstractResourceRegistration> snapshot = childRegistries;
        AbstractResourceRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getChildAddresses(iterator);
    }

    ProxyController getProxyController(final Iterator<PathElement> iterator, final String child) {
        final Map<String, AbstractResourceRegistration> snapshot = childRegistries;
        AbstractResourceRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getProxyController(iterator);
    }

    ManagementResourceRegistration getResourceRegistration(final Iterator<PathElement> iterator, final String child) {
        final Map<String, AbstractResourceRegistration> snapshot = childRegistries;
        AbstractResourceRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getResourceRegistration(iterator);
    }

    void getProxyControllers(final Iterator<PathElement> iterator, final String child, Set<ProxyController> controllers) {
        final Map<String, AbstractResourceRegistration> snapshot = childRegistries;
        if (child != null) {
            AbstractResourceRegistration childRegistry = snapshot.get(child);
            if (childRegistry == null) {
                childRegistry = snapshot.get("*");
                if (childRegistry == null) {
                    return;
                }
            }
            childRegistry.getProxyControllers(iterator, controllers);
        } else {
            for (AbstractResourceRegistration childRegistry : snapshot.values()) {
                childRegistry.getProxyControllers(iterator, controllers);
            }
        }
    }

}
