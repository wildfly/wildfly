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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.OperationEntry.EntryType;

/**
 * A registry of model node information.  This registry is thread-safe.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
abstract class AbstractResourceRegistration implements ManagementResourceRegistration {

    private final String valueString;
    private final NodeSubregistry parent;

    AbstractResourceRegistration(final String valueString, final NodeSubregistry parent) {
        this.valueString = valueString;
        this.parent = parent;
    }

    /** {@inheritDoc} */
    @Override
    public abstract ManagementResourceRegistration registerSubModel(final PathElement address, final DescriptionProvider descriptionProvider);

    /** {@inheritDoc} */
    @Override
    public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider) {
        registerOperationHandler(operationName, handler, descriptionProvider, false);
    }

    /** {@inheritDoc} */
    @Override
    public void registerOperationHandler(final String operationName, final OperationStepHandler handler, final DescriptionProvider descriptionProvider, final boolean inherited) {
        registerOperationHandler(operationName, handler, descriptionProvider, inherited, OperationEntry.EntryType.PUBLIC);
    }

    /** {@inheritDoc} */
    @Override
    public abstract void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, EntryType entryType);

    /** {@inheritDoc} */
    @Override
    public abstract void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, EntryType entryType, EnumSet<OperationEntry.Flag> flags);

    /** {@inheritDoc} */
    @Override
    public abstract void registerProxyController(final PathElement address, final ProxyController controller) throws IllegalArgumentException;

    /** {@inheritDoc} */
    @Override
    public abstract void unregisterProxyController(PathElement address);

    /**
     * Get a handler at a specific address.
     *
     * @param pathAddress the address
     * @param operationName the operation name
     * @return the operation handler, or {@code null} if none match
     */
    @Override
    public final OperationStepHandler getOperationHandler(final PathAddress pathAddress, final String operationName) {
        OperationStepHandler inheritable = getInheritableOperationHandler(operationName);
        OperationStepHandler result =  getOperationHandler(pathAddress.iterator(), operationName, inheritable);
        NodeSubregistry ancestorSubregistry = parent;
        while (result == null && ancestorSubregistry != null) {
            AbstractResourceRegistration ancestor = ancestorSubregistry.getParent();
            result = ancestor.getInheritableOperationHandler(operationName);
            ancestorSubregistry = ancestor.parent;
        }
        return result;
    }

    abstract OperationStepHandler getOperationHandler(ListIterator<PathElement> iterator, String operationName, OperationStepHandler inherited);
    abstract OperationStepHandler getInheritableOperationHandler(String operationName);

    @Override
    public AttributeAccess getAttributeAccess(final PathAddress address, final String attributeName) {
        return getAttributeAccess(address.iterator(), attributeName);
    }

    abstract AttributeAccess getAttributeAccess(final ListIterator<PathElement> address, final String attributeName);

    /**
     * Get all the handlers at a specific address.
     *
     * @param address the address
     * @param inherited true to include the inherited operations
     * @return the handlers
     */
    @Override
    public Map<String, OperationEntry> getOperationDescriptions(final PathAddress address, boolean inherited) {
        Map<String, OperationEntry> providers = new TreeMap<String, OperationEntry>();
        getOperationDescriptions(address.iterator(), providers, inherited);
        return providers;
    }

    abstract void getOperationDescriptions(ListIterator<PathElement> iterator, Map<String, OperationEntry> providers, boolean inherited);

    /** {@inheritDoc} */
    @Override
    public DescriptionProvider getOperationDescription(final PathAddress address, final String operationName) {
        DescriptionProvider inheritable = getInheritableOperationDescription(operationName);
        DescriptionProvider result = getOperationDescription(address.iterator(), operationName, inheritable);
        NodeSubregistry ancestorSubregistry = parent;
        while (result == null && ancestorSubregistry != null) {
            AbstractResourceRegistration ancestor = ancestorSubregistry.getParent();
            result = ancestor.getInheritableOperationDescription(operationName);
            ancestorSubregistry = ancestor.parent;
        }
        return result;
    }

    abstract DescriptionProvider getOperationDescription(Iterator<PathElement> iterator, String operationName, DescriptionProvider inherited);
    abstract DescriptionProvider getInheritableOperationDescription(String operationName);

    /**
     * Get a handler at a specific address.
     *
     * @param pathAddress the address
     * @param operationName the operation name
     * @return the operation handler, or {@code null} if none match
     */
    @Override
    public final Set<OperationEntry.Flag> getOperationFlags(final PathAddress pathAddress, final String operationName) {
        Set<OperationEntry.Flag> inheritable = getInheritableOperationFlags(operationName);
        Set<OperationEntry.Flag> result =  getOperationFlags(pathAddress.iterator(), operationName, inheritable);
        NodeSubregistry ancestorSubregistry = parent;
        while (result == null && ancestorSubregistry != null) {
            AbstractResourceRegistration ancestor = ancestorSubregistry.getParent();
            result = ancestor.getInheritableOperationFlags(operationName);
            ancestorSubregistry = ancestor.parent;
        }
        return result;
    }

    abstract Set<OperationEntry.Flag> getOperationFlags(ListIterator<PathElement> iterator, String operationName, Set<OperationEntry.Flag> inherited);

    abstract Set<OperationEntry.Flag> getInheritableOperationFlags(String operationName);

    /** {@inheritDoc} */
    @Override
    public DescriptionProvider getModelDescription(final PathAddress address) {
        return getModelDescription(address.iterator());
    }

    abstract DescriptionProvider getModelDescription(Iterator<PathElement> iterator);

    @Override
    public Set<String> getAttributeNames(final PathAddress address) {
        return getAttributeNames(address.iterator());
    }

    abstract Set<String> getAttributeNames(Iterator<PathElement> iterator);

    @Override
    public Set<String> getChildNames(final PathAddress address) {
        return getChildNames(address.iterator());
    }

    abstract Set<String> getChildNames(Iterator<PathElement> iterator);

    @Override
    public Set<PathElement> getChildAddresses(final PathAddress address){
        return getChildAddresses(address.iterator());
    }

    abstract Set<PathElement> getChildAddresses(Iterator<PathElement> iterator);

    public ProxyController getProxyController(final PathAddress address) {
        return getProxyController(address.iterator());
    }

    abstract ProxyController getProxyController(Iterator<PathElement> iterator);

    public Set<ProxyController> getProxyControllers(PathAddress address){
        Set<ProxyController> controllers = new HashSet<ProxyController>();
        getProxyControllers(address.iterator(), controllers);
        return controllers;
    }

    abstract void getProxyControllers(Iterator<PathElement> iterator, Set<ProxyController> controllers);

    /** {@inheritDoc} */
    @Override
    public ManagementResourceRegistration getSubModel(PathAddress address) {
        return getResourceRegistration(address.iterator());
    }

    abstract ManagementResourceRegistration getResourceRegistration(Iterator<PathElement> iterator);

    final String getValueString()  {
        return valueString;
    }

    final String getLocationString() {
        if (parent == null) {
            return "";
        } else {
            return parent.getLocationString() + valueString + ")";
        }
    }

    void getInheritedOperations(final Map<String, OperationEntry> providers, boolean skipSelf) {
        if (!skipSelf) {
            getInheritedOperationEntries(providers);
        }
        if (parent != null) {
            parent.getParent().getInheritedOperations(providers, false);
        }
    }

    abstract void getInheritedOperationEntries(final Map<String, OperationEntry> providers);
}
