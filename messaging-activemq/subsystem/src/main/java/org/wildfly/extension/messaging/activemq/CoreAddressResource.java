/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.ROLE;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.activemq.artemis.api.core.management.AddressControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;
import org.jboss.dmr.ModelNode;

/**
 * Resource for a runtime core ActiveMQ address.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class CoreAddressResource implements Resource {

    private final String name;
    private final ActiveMQBroker broker;

    public CoreAddressResource(final String name, final ActiveMQBroker broker) {
        this.name = name;
        this.broker = broker;
    }

    @Override
    public ModelNode getModel() {
        return new ModelNode();
    }

    @Override
    public void writeModel(ModelNode newModel) {
        throw MessagingLogger.ROOT_LOGGER.immutableResource();
    }

    @Override
    public boolean isModelDefined() {
        return false;
    }

    @Override
    public boolean hasChild(PathElement element) {
        return ROLE.equals(element.getKey()) && hasSecurityRole(element);
    }

    @Override
    public Resource getChild(PathElement element) {
        return hasSecurityRole(element) ? SecurityRoleResource.INSTANCE : null;
    }

    @Override
    public Resource requireChild(PathElement element) {
        Resource child = getChild(element);
        if (child != null) {
            return child;
        }
        throw new NoSuchResourceException(element);
    }

    @Override
    public boolean hasChildren(String childType) {
        return !getChildrenNames(childType).isEmpty();
    }

    @Override
    public Resource navigate(PathAddress address) {
        return Tools.navigate(this, address);
    }

    @Override
    public Set<String> getChildTypes() {
        return Collections.singleton(ROLE);
    }

    @Override
    public Set<String> getOrderedChildTypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (ROLE.equals(childType)) {
            return getSecurityRoles();
        }
        return Collections.emptySet();
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (ROLE.equals(childType)) {
            Set<ResourceEntry> result = new HashSet<ResourceEntry>();
            for (String name : getSecurityRoles()) {
                result.add(new SecurityRoleResource.SecurityRoleResourceEntry(name));
            }
            return result;
        }
        return Collections.emptySet();
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        throw MessagingLogger.ROOT_LOGGER.immutableResource();
    }

    @Override
    public void registerChild(PathElement pathElement, int i, Resource resource) {
        throw MessagingLogger.ROOT_LOGGER.immutableResource();
    }

    @Override
    public Resource removeChild(PathElement address) {
        throw MessagingLogger.ROOT_LOGGER.immutableResource();
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public Resource clone() {
        return new CoreAddressResource(name, broker);
    }

    private Set<String> getSecurityRoles() {
        AddressControl addressControl = getAddressControl();
        if (addressControl == null) {
            return Collections.emptySet();
        } else {
            try {
                return Stream.of(addressControl.getRoles()).map(objRole -> ((Object[])objRole)[0].toString()).collect(Collectors.toSet());
            } catch (Exception e) {
                return Collections.emptySet();
            }
        }
    }

    private AddressControl getAddressControl() {
        if (broker == null) {
            return null;
        }
        Object obj = broker.getResource(ResourceNames.ADDRESS + name);
        return AddressControl.class.cast(obj);
    }

    private boolean hasSecurityRole(PathElement element) {
        String role = element.getValue();
        return getSecurityRoles().contains(role);
    }

    public static class CoreAddressResourceEntry extends CoreAddressResource implements ResourceEntry {

        final PathElement path;
        // we keep a ref on the management service to be able to clone it... is there a more elegant way?
        private final ActiveMQBroker broker2;

        public CoreAddressResourceEntry(final String name, final ActiveMQBroker broker) {
            super(name, broker);
            broker2 = broker;
            path = PathElement.pathElement(CommonAttributes.CORE_ADDRESS, name);
        }

        @Override
        public String getName() {
            return path.getValue();
        }

        @Override
        public PathElement getPathElement() {
            return path;
        }

        @Override
        public CoreAddressResourceEntry clone() {
            return new CoreAddressResourceEntry(path.getValue(), broker2);
        }
    }
}
