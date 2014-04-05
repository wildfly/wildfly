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
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.NotificationDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyStepHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@SuppressWarnings("deprecation")
final class ProxyControllerRegistration extends AbstractResourceRegistration implements DescriptionProvider {

    @SuppressWarnings("unused")
    private volatile Map<String, OperationEntry> operations;

    @SuppressWarnings("unused")
    private volatile Map<String, AttributeAccess> attributes;

    private final ProxyController proxyController;
    private final OperationEntry operationEntry;

    private static final AtomicMapFieldUpdater<ProxyControllerRegistration, String, OperationEntry> operationsUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(ProxyControllerRegistration.class, Map.class, "operations"));
    private static final AtomicMapFieldUpdater<ProxyControllerRegistration, String, AttributeAccess> attributesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(ProxyControllerRegistration.class, Map.class, "attributes"));

    ProxyControllerRegistration(final String valueString, final NodeSubregistry parent, final ProxyController proxyController) {
        super(valueString, parent);
        this.operationEntry = new OperationEntry(new ProxyStepHandler(proxyController), this, false, OperationEntry.EntryType.PRIVATE);
        this.proxyController = proxyController;
        operationsUpdater.clear(this);
        attributesUpdater.clear(this);
    }

    @Override
    OperationEntry getOperationEntry(final ListIterator<PathElement> iterator, final String operationName, OperationEntry inherited) {
        checkPermission();
        if (! iterator.hasNext()) {
            // Only in case there is an explicit handler...
            final OperationEntry entry = operationsUpdater.get(this, operationName);
            return entry == null ? operationEntry : entry;
        } else {
            return operationEntry;
        }
    }

    @Override
    OperationEntry getInheritableOperationEntry(String operationName) {
        checkPermission();
        return null;
    }

    @Override
    public boolean isRuntimeOnly() {
        checkPermission();
        return true;
    }

    @Override
    public void setRuntimeOnly(final boolean runtimeOnly) {
        checkPermission();
    }

    @Override
    public boolean isRemote() {
        checkPermission();
        return true;
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        checkPermission();
        return Collections.emptyList();
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
    public void registerOperationHandler(final String operationName, final OperationStepHandler handler, final DescriptionProvider descriptionProvider, final boolean inherited, OperationEntry.EntryType entryType) {
        if (operationsUpdater.putIfAbsent(this, operationName, new OperationEntry(handler, descriptionProvider, inherited, entryType)) != null) {
            throw alreadyRegistered("operation handler", operationName);
        }
    }

    @Override
    public void registerOperationHandler(final String operationName, final OperationStepHandler handler, final DescriptionProvider descriptionProvider, final boolean inherited, OperationEntry.EntryType entryType, EnumSet<OperationEntry.Flag> flags) {
        if (operationsUpdater.putIfAbsent(this, operationName, new OperationEntry(handler, descriptionProvider, inherited, entryType, flags, null)) != null) {
            throw alreadyRegistered("operation handler", operationName);
        }
    }

    @Override
    public void registerOperationHandler(OperationDefinition definition, OperationStepHandler handler, boolean inherited) {
        if (operationsUpdater.putIfAbsent(this, definition.getName(), new OperationEntry(handler, definition.getDescriptionProvider(),
                inherited, definition.getEntryType(), definition.getFlags(), definition.getAccessConstraints())) != null) {
            throw alreadyRegistered("operation handler", definition.getName());
        }
    }

    @Override
    public void unregisterOperationHandler(final String operationName) {
        if (operationsUpdater.remove(this, operationName) == null) {
            throw operationNotRegisteredException(operationName, proxyController.getProxyNodeAddress().getLastElement());
        }
    }

    @Override
    public void registerReadWriteAttribute(final String attributeName, final OperationStepHandler readHandler, final OperationStepHandler writeHandler, AttributeAccess.Storage storage) {
        AttributeAccess aa = new AttributeAccess(AttributeAccess.AccessType.READ_WRITE, storage, readHandler, writeHandler, null, null);
        if (attributesUpdater.putIfAbsent(this, attributeName, aa) != null) {
            throw alreadyRegistered("attribute", attributeName);
        }
    }

    @Override
    public void registerReadWriteAttribute(final String attributeName, final OperationStepHandler readHandler, final OperationStepHandler writeHandler, EnumSet<AttributeAccess.Flag> flags) {
        AttributeAccess.Storage storage = (flags != null && flags.contains(AttributeAccess.Flag.STORAGE_RUNTIME)) ? AttributeAccess.Storage.RUNTIME : AttributeAccess.Storage.CONFIGURATION;
        AttributeAccess aa = new AttributeAccess(AttributeAccess.AccessType.READ_WRITE, storage, readHandler, writeHandler, null, flags);
        if (attributesUpdater.putIfAbsent(this, attributeName, aa) != null) {
            throw alreadyRegistered("attribute", attributeName);
        }
    }

    @Override
    public void registerReadWriteAttribute(final AttributeDefinition definition, final OperationStepHandler readHandler, final OperationStepHandler writeHandler) {
        final EnumSet<AttributeAccess.Flag> flags = definition.getFlags();
        final String attributeName = definition.getName();
        AttributeAccess.Storage storage = (flags != null && flags.contains(AttributeAccess.Flag.STORAGE_RUNTIME)) ? AttributeAccess.Storage.RUNTIME : AttributeAccess.Storage.CONFIGURATION;
        AttributeAccess aa = new AttributeAccess(AttributeAccess.AccessType.READ_WRITE, storage, readHandler, writeHandler, definition, flags);
        if (attributesUpdater.putIfAbsent(this, attributeName, aa) != null) {
            throw alreadyRegistered("attribute", attributeName);
        }
    }

    @Override
    public void registerReadOnlyAttribute(final String attributeName, final OperationStepHandler readHandler, AttributeAccess.Storage storage) {
        AttributeAccess aa = new AttributeAccess(AttributeAccess.AccessType.READ_ONLY, storage, readHandler, null, null, null);
        if (attributesUpdater.putIfAbsent(this, attributeName, aa) != null) {
            throw alreadyRegistered("attribute", attributeName);
        }
    }

    @Override
    public void registerReadOnlyAttribute(final String attributeName, final OperationStepHandler readHandler, EnumSet<AttributeAccess.Flag> flags) {
        AttributeAccess.Storage storage = (flags != null && flags.contains(AttributeAccess.Flag.STORAGE_RUNTIME)) ? AttributeAccess.Storage.RUNTIME : AttributeAccess.Storage.CONFIGURATION;
        AttributeAccess aa = new AttributeAccess(AttributeAccess.AccessType.READ_ONLY, storage, readHandler, null, null, null);
        if (attributesUpdater.putIfAbsent(this, attributeName, aa) != null) {
            throw alreadyRegistered("attribute", attributeName);
        }
    }

    @Override
    public void registerReadOnlyAttribute(final AttributeDefinition definition, final OperationStepHandler readHandler) {
        final EnumSet<AttributeAccess.Flag> flags = definition.getFlags();
        final String attributeName = definition.getName();
        AttributeAccess.Storage storage = (flags != null && flags.contains(AttributeAccess.Flag.STORAGE_RUNTIME)) ? AttributeAccess.Storage.RUNTIME : AttributeAccess.Storage.CONFIGURATION;
        AttributeAccess aa = new AttributeAccess(AttributeAccess.AccessType.READ_ONLY, storage, readHandler, null, definition, flags);
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
        AttributeAccess aa = new AttributeAccess(AttributeAccess.AccessType.METRIC, AttributeAccess.Storage.RUNTIME, metricHandler, null, null, flags);
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
        AttributeAccess aa = new AttributeAccess(AttributeAccess.AccessType.METRIC, AttributeAccess.Storage.RUNTIME, metricHandler, null, definition, definition.getFlags());
        if (attributesUpdater.putIfAbsent(this, definition.getName(), aa) != null) {
            throw alreadyRegistered("attribute", definition.getName());
        }
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
    public void registerAlias(PathElement address, AliasEntry alias) {
        throw alreadyRegistered();
    }

    @Override
    public void unregisterAlias(PathElement address) {
        throw alreadyRegistered();
    }

    @Override
    public void registerNotification(NotificationDefinition notification, boolean inherited) {
        throw alreadyRegistered();
    }

    @Override
    public void registerNotification(NotificationDefinition notification) {
        throw alreadyRegistered();
    }

    @Override
    public void unregisterNotification(String notificationType) {
        throw alreadyRegistered();
    }

    @Override
    void getOperationDescriptions(final ListIterator<PathElement> iterator, final Map<String, OperationEntry> providers, final boolean inherited) {
        checkPermission();
    }

    @Override
    void getInheritedOperationEntries(final Map<String, OperationEntry> providers) {
        checkPermission();
    }

    @Override
    void getNotificationDescriptions(ListIterator<PathElement> iterator, Map<String, NotificationEntry> providers, boolean inherited) {
        checkPermission();
    }

    @Override
    void getInheritedNotificationEntries(Map<String, NotificationEntry> providers) {
        checkPermission();
    }

    @Override
    DescriptionProvider getModelDescription(final ListIterator<PathElement> iterator) {
        checkPermission();
        return this;
    }

    @Override
    Set<String> getAttributeNames(final ListIterator<PathElement> iterator) {
        checkPermission();
        if (iterator.hasNext()) {
            return Collections.emptySet();
        } else {
            final Map<String, AttributeAccess> snapshot = attributesUpdater.get(this);
            return snapshot.keySet();
        }
    }

    @Override
    Set<String> getChildNames(final ListIterator<PathElement> iterator) {
        checkPermission();
        return Collections.emptySet();
    }

    @Override
    Set<PathElement> getChildAddresses(final ListIterator<PathElement> iterator) {
        checkPermission();
        return Collections.emptySet();
    }

    @Override
    AttributeAccess getAttributeAccess(final ListIterator<PathElement> iterator, final String attributeName) {
        checkPermission();
        if (iterator.hasNext()) {
            return null;
        } else {
            final Map<String, AttributeAccess> snapshot = attributesUpdater.get(this);
            return snapshot.get(attributeName);
        }
    }

    @Override
    ProxyController getProxyController(ListIterator<PathElement> iterator) {
        checkPermission();
        return proxyController;
    }

    @Override
    void getProxyControllers(ListIterator<PathElement> iterator, Set<ProxyController> controllers) {
        checkPermission();
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
        checkPermission();
        return this;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        checkPermission();
        //TODO
        return new ModelNode();
    }

    private IllegalArgumentException alreadyRegistered() {
        return MESSAGES.proxyHandlerAlreadyRegistered(getLocationString());
    }

    private IllegalArgumentException alreadyRegistered(final String type, final String name) {
        return MESSAGES.alreadyRegistered(type, name, getLocationString());
    }

    private IllegalArgumentException operationNotRegisteredException(String op, PathElement address) {
        return MESSAGES.operationNotRegisteredException(op, PathAddress.pathAddress(address));
    }

    @Override
    public AliasEntry getAliasEntry() {
        checkPermission();
        return null;
    }

    @Override
    protected void registerAlias(PathElement address, AliasEntry alias, AbstractResourceRegistration target) {
        throw MESSAGES.proxyHandlerAlreadyRegistered(getLocationString());
    }
}
