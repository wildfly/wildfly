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

package org.jboss.as.messaging;

import static org.jboss.as.messaging.CommonAttributes.NAME;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_ROLE;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hornetq.api.core.management.AddressControl;
import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.core.server.management.ManagementService;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Resource for a runtime core HornetQ address.
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
        throw MESSAGES.immutableResource();
    }

    @Override
    public boolean isModelDefined() {
        return false;
    }

    @Override
    public boolean hasChild(PathElement element) {
        if (CommonAttributes.SECURITY_ROLE.equals(element.getKey())) {
            return hasSecurityRole(element);
        }
        return false;
    }

    @Override
    public Resource getChild(PathElement element) {
        return hasSecurityRole(element) ? SecurityRoleResource.INSTANCE : null;
    }

    @Override
    public Resource requireChild(PathElement element) {
        throw new NoSuchResourceException(element);
    }

    @Override
    public boolean hasChildren(String childType) {
        return false;
    }

    @Override
    public Resource navigate(PathAddress address) {
        return Resource.Tools.navigate(this, address);
    }

    @Override
    public Set<String> getChildTypes() {
        Set<String> result = new HashSet<String>();
        result.add(SECURITY_ROLE);
        return result;
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (SECURITY_ROLE.equals(childType)) {
            return getSecurityRoles();
        }
        return Collections.emptySet();
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (SECURITY_ROLE.equals(childType)) {
            Set<ResourceEntry> result = new HashSet<ResourceEntry>();
            for (String name : getSecurityRoles()) {
                result.add(new SecurityRoleResource.SecuriyRoleResourceEntry(name));
            }
            return result;
        }
        return Collections.emptySet();
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        throw MESSAGES.immutableResource();
    }

    @Override
    public Resource removeChild(PathElement address) {
        throw MESSAGES.immutableResource();
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
        Object obj = managementService.getResource(ResourceNames.CORE_ADDRESS + name);
        AddressControl control = AddressControl.class.cast(obj);
        return control;
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
