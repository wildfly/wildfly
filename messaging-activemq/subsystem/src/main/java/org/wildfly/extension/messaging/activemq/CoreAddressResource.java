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

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.NAME;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.ROLE;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.activemq.artemis.api.core.management.AddressControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.core.server.management.ManagementService;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;

/**
 * Resource for a runtime core ActiveMQ address.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class CoreAddressResource implements Resource {

    private final String name;
    private final ManagementService managementService;

    public CoreAddressResource(final String name, final ManagementService managementService) {
        this.name = name;
        this.managementService = managementService;
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
        return getChildrenNames(childType).size() > 0;
    }

    @Override
    public Resource navigate(PathAddress address) {
        return Resource.Tools.navigate(this, address);
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
        return new CoreAddressResource(name, managementService);
    }

    private Set<String> getSecurityRoles() {
        AddressControl addressControl = getAddressControl();
        if (addressControl == null) {
            return Collections.emptySet();
        } else {
            Set<String> names = new HashSet<String>();
            try {
                ModelNode res = ModelNode.fromJSONString(addressControl.getRolesAsJSON());
                ModelNode converted = ManagementUtil.convertSecurityRole(res);
                for (ModelNode role : converted.asList()) {
                    names.add(role.get(NAME).asString());
                }
                return names;
            } catch (Exception e) {
                return Collections.emptySet();
            }
        }
    }

    private AddressControl getAddressControl() {
        if (managementService == null) {
            return null;
        }
        Object obj = managementService.getResource(ResourceNames.ADDRESS + name);
        return AddressControl.class.cast(obj);
    }

    private boolean hasSecurityRole(PathElement element) {
        String role = element.getValue();
        return getSecurityRoles().contains(role);
    }

    public static class CoreAddressResourceEntry extends CoreAddressResource implements ResourceEntry {

        final PathElement path;
        // we keep a ref on the management service to be able to clone it... is there a more elegant way?
        private final ManagementService managementService2;

        public CoreAddressResourceEntry(final String name, final ManagementService managementService) {
            super(name, managementService);
            managementService2 = managementService;
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
            return new CoreAddressResourceEntry(path.getValue(), managementService2);
        }
    }
}
