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
import java.util.EnumSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ProxyControllerRegistration extends AbstractResourceRegistration implements DescriptionProvider {

    private final ProxyController proxyController;

    ProxyControllerRegistration(final String valueString, final NodeSubregistry parent, final ProxyController proxyController) {
        super(valueString, parent);
        this.proxyController = proxyController;
    }

    @Override
    OperationStepHandler getOperationHandler(final ListIterator<PathElement> iterator, final String operationName, OperationStepHandler inherited) {
        return new ProxyStepHandler(proxyController);
    }

    @Override
    OperationStepHandler getInheritableOperationHandler(String operationName) {
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
    public ManagementResourceRegistration registerSubModel(final PathElement address, final DescriptionProvider descriptionProvider) {
        throw new IllegalArgumentException("A proxy handler is already registered at location '" + getLocationString() + "'");
    }

    @Override
    public void registerSubModel(final PathElement address, final ManagementResourceRegistration subModel) {
        throw new IllegalArgumentException("A proxy handler is already registered at location '" + getLocationString() + "'");
    }

    @Override
    public void registerOperationHandler(final String operationName, final OperationStepHandler handler, final DescriptionProvider descriptionProvider, final boolean inherited, EntryType entryType) {
        throw new IllegalArgumentException("A proxy handler is already registered at location '" + getLocationString() + "'");
    }

    @Override
    public void registerOperationHandler(final String operationName, final OperationStepHandler handler, final DescriptionProvider descriptionProvider, final boolean inherited, EntryType entryType, EnumSet<OperationEntry.Flag> flags) {
        throw new IllegalArgumentException("A proxy handler is already registered at location '" + getLocationString() + "'");
    }

    @Override
    public void registerReadWriteAttribute(final String attributeName, final OperationStepHandler readHandler, final OperationStepHandler writeHandler, AttributeAccess.Storage storage) {
        throw new IllegalArgumentException("A proxy handler is already registered at location '" + getLocationString() + "'");
    }

    @Override
    public void registerReadOnlyAttribute(final String attributeName, final OperationStepHandler readHandler, AttributeAccess.Storage storage) {
        throw new IllegalArgumentException("A proxy handler is already registered at location '" + getLocationString() + "'");
    }

    @Override
    public void registerMetric(final String attributeName, final OperationStepHandler metricHandler) {
        throw new IllegalArgumentException("A proxy handler is already registered at location '" + getLocationString() + "'");
    }

    @Override
    public void registerProxyController(final PathElement address, final ProxyController proxyController) throws IllegalArgumentException {
        throw new IllegalArgumentException("A proxy handler is already registered at location '" + getLocationString() + "'");
    }

    @Override
    public void unregisterProxyController(final PathElement address) throws IllegalArgumentException {
        throw new IllegalArgumentException("A proxy handler is already registered at location '" + getLocationString() + "'");
    }

    @Override
    void getOperationDescriptions(final ListIterator<PathElement> iterator, final Map<String, OperationEntry> providers, final boolean inherited) {

    }

    @Override
    void getInheritedOperationEntries(final Map<String, OperationEntry> providers) {
    }

    @Override
    DescriptionProvider getOperationDescription(final Iterator<PathElement> iterator, final String operationName, DescriptionProvider inherited) {
        return null;
    }

    @Override
    DescriptionProvider getInheritableOperationDescription(String operationName) {
        return null;
    }

    @Override
    Set<OperationEntry.Flag> getOperationFlags(ListIterator<PathElement> iterator, String operationName, Set<OperationEntry.Flag> inherited) {
        return Collections.emptySet();
    }

    @Override
    Set<OperationEntry.Flag> getInheritableOperationFlags(String operationName) {
        return Collections.emptySet();
    }

    @Override
    DescriptionProvider getModelDescription(final Iterator<PathElement> iterator) {
        return null;
    }

    @Override
    Set<String> getAttributeNames(final Iterator<PathElement> iterator) {
        return Collections.emptySet();
    }

    @Override
    Set<String> getChildNames(final Iterator<PathElement> iterator) {
        return Collections.emptySet();
    }

    @Override
    Set<PathElement> getChildAddresses(final Iterator<PathElement> iterator) {
        return Collections.emptySet();
    }

    @Override
    AttributeAccess getAttributeAccess(final ListIterator<PathElement> address, final String attributeName) {
        return null;
    }

    @Override
    ProxyController getProxyController(Iterator<PathElement> iterator) {
        return proxyController;
    }

    @Override
    void getProxyControllers(Iterator<PathElement> iterator, Set<ProxyController> controllers) {
        controllers.add(proxyController);
    }

    @Override
    ManagementResourceRegistration getResourceRegistration(Iterator<PathElement> iterator) {
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
        //return proxyController.execute(operation, handler, control, attachments);
        return new ModelNode();
    }
}
