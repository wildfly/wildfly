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
import java.util.EnumSet;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyStepHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ProxyControllerRegistration extends AbstractResourceRegistration implements DescriptionProvider {

    private final ProxyController proxyController;
    private final OperationEntry operationEntry;

    ProxyControllerRegistration(final String valueString, final NodeSubregistry parent, final ProxyController proxyController) {
        super(valueString, parent);
        this.operationEntry = new OperationEntry(new ProxyStepHandler(proxyController), this, false, EntryType.PRIVATE);
        this.proxyController = proxyController;
    }

    @Override
    OperationEntry getOperationEntry(final ListIterator<PathElement> iterator, final String operationName, OperationEntry inherited) {
        return operationEntry;
    }

    @Override
    OperationEntry getInheritableOperationEntry(String operationName) {
        return null;
    }

    @Override
    public boolean isRuntimeOnly() {
        return true;
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public ManagementResourceRegistration registerSubModel(final ResourceDefinition resourceDefinition) {
        throw alreadyRegistered();
    }

    @Override
    public void unregisterSubModel(final PathElement address) throws IllegalArgumentException {
        throw alreadyRegistered();
    }

    @Override
    public ManagementResourceRegistration registerOverrideModel(String name, OverrideDescriptionProvider descriptionProvider) {
        throw alreadyRegistered();
    }

    @Override
    public void unregisterOverrideModel(String name) {
        throw alreadyRegistered();
    }

    @Override
    public void registerOperationHandler(final String operationName, final OperationStepHandler handler, final DescriptionProvider descriptionProvider, final boolean inherited, EntryType entryType) {
        throw alreadyRegistered();
    }

    @Override
    public void registerOperationHandler(final String operationName, final OperationStepHandler handler, final DescriptionProvider descriptionProvider, final boolean inherited, EntryType entryType, EnumSet<OperationEntry.Flag> flags) {
        throw alreadyRegistered();
    }

    @Override
    public void registerReadWriteAttribute(final String attributeName, final OperationStepHandler readHandler, final OperationStepHandler writeHandler, AttributeAccess.Storage storage) {
        throw alreadyRegistered();
    }

    @Override
    public void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler, OperationStepHandler writeHandler, EnumSet<AttributeAccess.Flag> flags) {
        throw alreadyRegistered();
    }

    @Override
    public void registerReadWriteAttribute(AttributeDefinition definition, OperationStepHandler readHandler, OperationStepHandler writeHandler) {
        throw alreadyRegistered();
    }

    @Override
    public void registerReadOnlyAttribute(final String attributeName, final OperationStepHandler readHandler, AttributeAccess.Storage storage) {
        throw alreadyRegistered();
    }

    @Override
    public void registerReadOnlyAttribute(String attributeName, OperationStepHandler readHandler, EnumSet<AttributeAccess.Flag> flags) {
        throw alreadyRegistered();
    }

    @Override
    public void registerReadOnlyAttribute(AttributeDefinition definition, OperationStepHandler readHandler) {
        throw alreadyRegistered();
    }

    @Override
    public void registerMetric(final String attributeName, final OperationStepHandler metricHandler) {
        throw alreadyRegistered();
    }

    @Override
    public void registerMetric(AttributeDefinition definition, OperationStepHandler metricHandler) {
        throw alreadyRegistered();
    }

    @Override
    public void registerMetric(String attributeName, OperationStepHandler metricHandler, EnumSet<AttributeAccess.Flag> flags) {
        throw alreadyRegistered();
    }

    @Override
    public void unregisterMetric(String attributeName) {
        alreadyRegistered();
    }

    @Override
    public void registerProxyController(final PathElement address, final ProxyController proxyController) throws IllegalArgumentException {
        throw alreadyRegistered();
    }

    @Override
    public void unregisterProxyController(final PathElement address) throws IllegalArgumentException {
        throw alreadyRegistered();
    }

    @Override
    void getOperationDescriptions(final ListIterator<PathElement> iterator, final Map<String, OperationEntry> providers, final boolean inherited) {

    }

    @Override
    void getInheritedOperationEntries(final Map<String, OperationEntry> providers) {
    }

    @Override
    DescriptionProvider getModelDescription(final ListIterator<PathElement> iterator) {
        return null;
    }

    @Override
    Set<String> getAttributeNames(final ListIterator<PathElement> iterator) {
        return Collections.emptySet();
    }

    @Override
    Set<String> getChildNames(final ListIterator<PathElement> iterator) {
        return Collections.emptySet();
    }

    @Override
    Set<PathElement> getChildAddresses(final ListIterator<PathElement> iterator) {
        return Collections.emptySet();
    }

    @Override
    AttributeAccess getAttributeAccess(final ListIterator<PathElement> address, final String attributeName) {
        return null;
    }

    @Override
    ProxyController getProxyController(ListIterator<PathElement> iterator) {
        return proxyController;
    }

    @Override
    void getProxyControllers(ListIterator<PathElement> iterator, Set<ProxyController> controllers) {
        controllers.add(proxyController);
    }

    @Override
    AbstractResourceRegistration getResourceRegistration(ListIterator<PathElement> iterator) {
        // BES 2011/06/14 I do not see why the IAE makes sense, so...
//        if (!iterator.hasNext()) {
//            return this;
//        }
//        throw new IllegalArgumentException("Can't get child registrations of a proxy");
        while (iterator.hasNext())
            iterator.next();
        return this;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        //TODO
        return new ModelNode();
    }

    private IllegalArgumentException alreadyRegistered() {
        return MESSAGES.proxyHandlerAlreadyRegistered(getLocationString());
    }
}
