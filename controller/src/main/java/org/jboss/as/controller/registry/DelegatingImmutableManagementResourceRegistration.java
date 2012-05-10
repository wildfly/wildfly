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

import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.descriptions.DescriptionProvider;

/**
 * {@link ImmutableManagementResourceRegistration} implementation that simply delegates to another
 * {@link ImmutableManagementResourceRegistration} (typically a mutable implementation of sub-interface
 * {@link ManagementResourceRegistration}).
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DelegatingImmutableManagementResourceRegistration implements ImmutableManagementResourceRegistration {

    private final ImmutableManagementResourceRegistration delegate;

    /**
     * Creates a new ImmutableManagementResourceRegistration.
     *
     * @param delegate the delegate. Cannot be {@code null}
     */
    public DelegatingImmutableManagementResourceRegistration(ImmutableManagementResourceRegistration delegate) {
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
    public ProxyController getProxyController(PathAddress address) {
        return delegate.getProxyController(address);
    }

    @Override
    public Set<ProxyController> getProxyControllers(PathAddress address) {
        return delegate.getProxyControllers(address);
    }

    @Override
    public ImmutableManagementResourceRegistration getSubModel(PathAddress address) {
        ImmutableManagementResourceRegistration sub = delegate.getSubModel(address);
        return sub == null ? null : new DelegatingImmutableManagementResourceRegistration(sub);
    }

    @Override
    public AliasEntry getAliasEntry() {
        return delegate.getAliasEntry();
    }
}
