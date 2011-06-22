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

import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;

/**
 * A registry of values within a specific key type.
 */
final class NodeSubregistry {

    private final String keyName;
    private final ConcreteNodeRegistration parent;
    @SuppressWarnings( { "unused" })
    private volatile Map<String, AbstractNodeRegistration> childRegistries;

    private static final AtomicMapFieldUpdater<NodeSubregistry, String, AbstractNodeRegistration> childRegistriesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(NodeSubregistry.class, Map.class, "childRegistries"));

    NodeSubregistry(final String keyName, final ConcreteNodeRegistration parent) {
        this.keyName = keyName;
        this.parent = parent;
        childRegistriesUpdater.clear(this);
    }

    AbstractNodeRegistration getParent() {
        return parent;
    }

    Set<String> getChildNames(){
        final Map<String, AbstractNodeRegistration> snapshot = this.childRegistries;
        if (snapshot == null) {
            return Collections.emptySet();
        }
        return new HashSet<String>(snapshot.keySet());
    }

    ModelNodeRegistration register(final String elementValue, final DescriptionProvider provider, boolean runtimeOnly) {
        final AbstractNodeRegistration newRegistry = new ConcreteNodeRegistration(elementValue, this, provider, runtimeOnly);
        register(elementValue, newRegistry);
        return newRegistry;
    }

    void register(final String elementValue, final ModelNodeRegistration subModel) {
        AbstractNodeRegistration newRegistry = null;
        if (subModel instanceof AbstractNodeRegistration) {
            newRegistry = (AbstractNodeRegistration) subModel;
        }
        else {

        }
        final AbstractNodeRegistration appearingRegistry = childRegistriesUpdater.putIfAbsent(this, elementValue, newRegistry);
        if (appearingRegistry != null) {
            throw new IllegalArgumentException("A node is already registered at '" + getLocationString() + elementValue + ")'");
        }
    }

    ProxyControllerRegistration registerProxyController(final String elementValue, final NewProxyController proxyController) {
        final ProxyControllerRegistration newRegistry = new ProxyControllerRegistration(elementValue, this, proxyController);
        final AbstractNodeRegistration appearingRegistry = childRegistriesUpdater.putIfAbsent(this, elementValue, newRegistry);
        if (appearingRegistry != null) {
            throw new IllegalArgumentException("A node is already registered at '" + getLocationString() + elementValue + ")'");
        }
        //register(elementValue, newRegistry);
        return newRegistry;
    }

    void unregisterProxyController(final String elementValue) {
        childRegistriesUpdater.remove(this, elementValue);
    }

    NewStepHandler getOperationHandler(final ListIterator<PathElement> iterator, final String child, final String operationName, NewStepHandler inherited) {
        final Map<String, AbstractNodeRegistration> snapshot = childRegistriesUpdater.get(this);
        final AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry != null) {
            return childRegistry.getOperationHandler(iterator, operationName, inherited);
        } else {
            final AbstractNodeRegistration wildcardRegistry = snapshot.get("*");
            if (wildcardRegistry != null) {
                return wildcardRegistry.getOperationHandler(iterator, operationName, inherited);
            } else {
                return null;
            }
        }
    }

    void getHandlers(final ListIterator<PathElement> iterator, final String child, final Map<String, OperationEntry> providers, final boolean inherited) {
        final Map<String, AbstractNodeRegistration> snapshot = childRegistriesUpdater.get(this);
        final AbstractNodeRegistration childRegistry = snapshot.get(child);
        final AbstractNodeRegistration wildcardRegistry = snapshot.get("*");
        if (wildcardRegistry == null) {
            if (childRegistry == null) {
                return;
            } else {
                childRegistry.getOperationDescriptions(iterator, providers, inherited);
                return;
            }
        } else {
            if (childRegistry == null) {
                wildcardRegistry.getOperationDescriptions(iterator, providers, inherited);
                return;
            } else {
                wildcardRegistry.getOperationDescriptions(iterator, providers, inherited);
                childRegistry.getOperationDescriptions(iterator, providers, inherited);
            }
        }
    }

    String getLocationString() {
        return parent.getLocationString() + "(" + keyName + " => ";
    }

    DescriptionProvider getOperationDescription(final Iterator<PathElement> iterator, final String child, final String operationName, DescriptionProvider inherited) {
        final Map<String, AbstractNodeRegistration> snapshot = childRegistries;
        AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry != null) {
            return childRegistry.getOperationDescription(iterator, operationName, inherited);
        } else {
            final AbstractNodeRegistration wildcardRegistry = snapshot.get("*");
            if (wildcardRegistry != null) {
                return wildcardRegistry.getOperationDescription(iterator, operationName, inherited);
            } else {
                return null;
            }
        }
    }

    Set<OperationEntry.Flag> getOperationFlags(final ListIterator<PathElement> iterator, final String child, final String operationName, Set<OperationEntry.Flag> inherited) {
        final Map<String, AbstractNodeRegistration> snapshot = childRegistriesUpdater.get(this);
        final AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry != null) {
            return childRegistry.getOperationFlags(iterator, operationName, inherited);
        } else {
            final AbstractNodeRegistration wildcardRegistry = snapshot.get("*");
            if (wildcardRegistry != null) {
                return wildcardRegistry.getOperationFlags(iterator, operationName, inherited);
            } else {
                return null;
            }
        }
    }

    DescriptionProvider getModelDescription(final Iterator<PathElement> iterator, final String child) {
        final Map<String, AbstractNodeRegistration> snapshot = childRegistries;
        AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getModelDescription(iterator);
    }

    Set<String> getChildNames(final Iterator<PathElement> iterator, final String child){
        final Map<String, AbstractNodeRegistration> snapshot = childRegistries;
        AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getChildNames(iterator);
    }

    Set<String> getAttributeNames(final Iterator<PathElement> iterator, final String child){
        final Map<String, AbstractNodeRegistration> snapshot = childRegistries;
        AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getAttributeNames(iterator);
    }

    AttributeAccess getAttributeAccess(final ListIterator<PathElement> iterator, final String child, final String attributeName) {
        final Map<String, AbstractNodeRegistration> snapshot = childRegistriesUpdater.get(this);
        final AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry != null) {
            return childRegistry.getAttributeAccess(iterator, attributeName);
        } else {
            final AbstractNodeRegistration wildcardRegistry = snapshot.get("*");
            if (wildcardRegistry != null) {
                return wildcardRegistry.getAttributeAccess(iterator, attributeName);
            } else {
                return null;
            }
        }
    }


    Set<PathElement> getChildAddresses(final Iterator<PathElement> iterator, final String child){
        final Map<String, AbstractNodeRegistration> snapshot = childRegistries;
        AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getChildAddresses(iterator);
    }

    NewProxyController getProxyController(final Iterator<PathElement> iterator, final String child) {
        final Map<String, AbstractNodeRegistration> snapshot = childRegistries;
        AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            //Don't handle '*' for now
            return null;
        }
        return childRegistry.getProxyController(iterator);
    }

    ModelNodeRegistration getModelNodeRegistration(final Iterator<PathElement> iterator, final String child) {
        final Map<String, AbstractNodeRegistration> snapshot = childRegistries;
        AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getNodeRegistration(iterator);
    }

    void getProxyControllers(final Iterator<PathElement> iterator, final String child, Set<NewProxyController> controllers) {
        final Map<String, AbstractNodeRegistration> snapshot = childRegistries;
        if (child != null) {
            AbstractNodeRegistration childRegistry = snapshot.get(child);
            if (childRegistry == null) {
                //Don't handle '*' for now
                return;
            }
            childRegistry.getProxyControllers(iterator, controllers);
        } else {
            for (AbstractNodeRegistration childRegistry : snapshot.values()) {
                childRegistry.getProxyControllers(iterator, controllers);
            }
        }
    }

}
