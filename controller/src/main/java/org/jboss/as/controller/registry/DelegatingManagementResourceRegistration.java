/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.NotificationDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;

/**
 * {@link ManagementResourceRegistration} implementation that simply delegates to another
 * {@link ManagementResourceRegistration}. Intended as a convenience class to allow overriding
 * of standard behaviors.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
@SuppressWarnings("deprecation")
public class DelegatingManagementResourceRegistration implements ManagementResourceRegistration {

    private final ManagementResourceRegistration delegate;

    /**
     * Creates a new DelegatingManagementResourceRegistration.
     *
     * @param delegate the delegate. Cannot be {@code null}
     */
    public DelegatingManagementResourceRegistration(ManagementResourceRegistration delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isRuntimeOnly() {
        return delegate.isRuntimeOnly();
    }

    @Override
    public boolean isRemote() {
        return delegate.isRemote();
    }

    @Override
    public boolean isAlias() {
        return delegate.isAlias();
    }

    @Override
    public OperationEntry getOperationEntry(PathAddress address, String operationName) {
        return delegate.getOperationEntry(address, operationName);
    }

    @Override
    public OperationStepHandler getOperationHandler(PathAddress address, String operationName) {
        return delegate.getOperationHandler(address, operationName);
    }

    @Override
    public DescriptionProvider getOperationDescription(PathAddress address, String operationName) {
        return delegate.getOperationDescription(address, operationName);
    }

    @Override
    public Set<OperationEntry.Flag> getOperationFlags(PathAddress address, String operationName) {
        return delegate.getOperationFlags(address, operationName);
    }

    @Override
    public Set<String> getAttributeNames(PathAddress address) {
        return delegate.getAttributeNames(address);
    }

    @Override
    public AttributeAccess getAttributeAccess(PathAddress address, String attributeName) {
        return delegate.getAttributeAccess(address, attributeName);
    }

    @Override
    public Set<String> getChildNames(PathAddress address) {
        return delegate.getChildNames(address);
    }

    @Override
    public Set<PathElement> getChildAddresses(PathAddress address) {
        return delegate.getChildAddresses(address);
    }

    @Override
    public DescriptionProvider getModelDescription(PathAddress address) {
        return delegate.getModelDescription(address);
    }

    @Override
    public Map<String, OperationEntry> getOperationDescriptions(PathAddress address, boolean inherited) {
        return delegate.getOperationDescriptions(address, inherited);
    }

    @Override
    public Map<String, NotificationEntry> getNotificationDescriptions(PathAddress address, boolean inherited) {
        return delegate.getNotificationDescriptions(address, inherited);
    }

    @Override
    public ProxyController getProxyController(PathAddress address) {
        return delegate.getProxyController(address);
    }

    @Override
    public Set<ProxyController> getProxyControllers(PathAddress address) {
        return delegate.getProxyControllers(address);
    }

    @Override
    public ManagementResourceRegistration getOverrideModel(String name) {
        return delegate.getOverrideModel(name);
    }

    @Override
    public ManagementResourceRegistration getSubModel(PathAddress address) {
        return delegate.getSubModel(address);
    }

    @Override
    public ManagementResourceRegistration registerSubModel(PathElement address, DescriptionProvider descriptionProvider) {
        return delegate.registerSubModel(address, descriptionProvider);
    }

    @Override
    public ManagementResourceRegistration registerSubModel(ResourceDefinition resourceDefinition) {
        return delegate.registerSubModel(resourceDefinition);
    }

    @Override
    public void unregisterSubModel(PathElement address) {
        delegate.unregisterSubModel(address);
    }

    @Override
    public boolean isAllowsOverride() {
        return delegate.isAllowsOverride();
    }

    @Override
    public void setRuntimeOnly(boolean runtimeOnly) {
        delegate.setRuntimeOnly(runtimeOnly);
    }

    @Override
    public ManagementResourceRegistration registerOverrideModel(String name, OverrideDescriptionProvider descriptionProvider) {
        return delegate.registerOverrideModel(name, descriptionProvider);
    }

    @Override
    public void unregisterOverrideModel(String name) {
        delegate.unregisterOverrideModel(name);
    }

    @Override
    public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider) {
        delegate.registerOperationHandler(operationName, handler, descriptionProvider);
    }

    @Override
    public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, EnumSet<OperationEntry.Flag> flags) {
        delegate.registerOperationHandler(operationName, handler, descriptionProvider, flags);
    }

    @Override
    public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited) {
        delegate.registerOperationHandler(operationName, handler, descriptionProvider, inherited);
    }

    @Override
    public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, OperationEntry.EntryType entryType) {
        delegate.registerOperationHandler(operationName, handler, descriptionProvider, inherited, entryType);
    }

    @Override
    public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, EnumSet<OperationEntry.Flag> flags) {
        delegate.registerOperationHandler(operationName, handler, descriptionProvider, inherited, flags);
    }

    @Override
    public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, OperationEntry.EntryType entryType, EnumSet<OperationEntry.Flag> flags) {
        delegate.registerOperationHandler(operationName, handler, descriptionProvider, inherited, entryType, flags);
    }

    @Override
    public void registerOperationHandler(OperationDefinition definition, OperationStepHandler handler) {
        delegate.registerOperationHandler(definition, handler);
    }

    @Override
    public void registerOperationHandler(OperationDefinition definition, OperationStepHandler handler, boolean inherited) {
        delegate.registerOperationHandler(definition, handler, inherited);
    }

    @Override
    public void unregisterOperationHandler(String operationName) {
        delegate.unregisterOperationHandler(operationName);
    }

    @Override
    public void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler, OperationStepHandler writeHandler, AttributeAccess.Storage storage) {
        delegate.registerReadWriteAttribute(attributeName, readHandler, writeHandler, storage);
    }

    @Override
    public void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler, OperationStepHandler writeHandler, EnumSet<AttributeAccess.Flag> flags) {
        delegate.registerReadWriteAttribute(attributeName, readHandler, writeHandler, flags);
    }

    @Override
    public void registerReadWriteAttribute(AttributeDefinition definition, OperationStepHandler readHandler, OperationStepHandler writeHandler) {
        delegate.registerReadWriteAttribute(definition, readHandler, writeHandler);
    }

    @Override
    public void registerReadOnlyAttribute(String attributeName, OperationStepHandler readHandler, AttributeAccess.Storage storage) {
        delegate.registerReadOnlyAttribute(attributeName, readHandler, storage);
    }

    @Override
    public void registerReadOnlyAttribute(String attributeName, OperationStepHandler readHandler, EnumSet<AttributeAccess.Flag> flags) {
        delegate.registerReadOnlyAttribute(attributeName, readHandler, flags);
    }

    @Override
    public void registerReadOnlyAttribute(AttributeDefinition definition, OperationStepHandler readHandler) {
        delegate.registerReadOnlyAttribute(definition, readHandler);
    }

    @Override
    public void registerMetric(String attributeName, OperationStepHandler metricHandler) {
        delegate.registerMetric(attributeName, metricHandler);
    }

    @Override
    public void registerMetric(AttributeDefinition definition, OperationStepHandler metricHandler) {
        delegate.registerMetric(definition, metricHandler);
    }

    @Override
    public void registerMetric(String attributeName, OperationStepHandler metricHandler, EnumSet<AttributeAccess.Flag> flags) {
        delegate.registerMetric(attributeName, metricHandler, flags);
    }

    @Override
    public void unregisterAttribute(String attributeName) {
        delegate.unregisterAttribute(attributeName);
    }

    @Override
    public void registerProxyController(PathElement address, ProxyController proxyController) {
        delegate.registerProxyController(address, proxyController);
    }

    @Override
    public void unregisterProxyController(PathElement address) {
        delegate.unregisterProxyController(address);
    }

    @Override
    public void registerAlias(PathElement address, AliasEntry aliasEntry) {
        delegate.registerAlias(address, aliasEntry);
    }

    @Override
    public void unregisterAlias(PathElement address) {
        delegate.unregisterAlias(address);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return delegate.getAccessConstraints();
    }

    @Override
    public AliasEntry getAliasEntry() {
        return delegate.getAliasEntry();
    }

    @Override
    public void registerNotification(NotificationDefinition notification, boolean inherited) {
        delegate.registerNotification(notification, inherited);
    }

    @Override
    public void registerNotification(NotificationDefinition notification) {
        delegate.registerNotification(notification);
    }

    @Override
    public void unregisterNotification(String notificationType) {
        delegate.unregisterNotification(notificationType);
    }
}
