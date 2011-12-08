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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.jboss.as.messaging.CommonAttributes.CORE_ADDRESS;

import org.hornetq.api.core.management.AddressControl;
import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.management.ManagementService;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Resource representing a HornetQ server.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HornetQServerResource implements Resource {

    private final Resource delegate;
    private ServiceController<HornetQServer> hornetQServerServiceController;

//    private ManagementService managementService;

    public HornetQServerResource() {
        this(Resource.Factory.create());
    }

    public HornetQServerResource(final Resource delegate) {
        this.delegate = delegate;
    }

    public ServiceController<HornetQServer> getHornetQServerServiceController() {
        return hornetQServerServiceController;
    }

    public void setHornetQServerServiceController(ServiceController<HornetQServer> hornetQServerServiceController) {
        this.hornetQServerServiceController = hornetQServerServiceController;
    }

    @Override
    public ModelNode getModel() {
        return delegate.getModel();
    }

    @Override
    public void writeModel(ModelNode newModel) {
        delegate.writeModel(newModel);
    }

    @Override
    public boolean isModelDefined() {
        return delegate.isModelDefined();
    }

    @Override
    public boolean hasChild(PathElement element) {
        if (CORE_ADDRESS.equals(element.getKey())) {
            return hasAddressControl(element);
        } else {
            return delegate.hasChild(element);
        }
    }

    @Override
    public Resource getChild(PathElement element) {
        if (CORE_ADDRESS.equals(element.getKey())) {
            return hasAddressControl(element) ? CoreAddressResource.INSTANCE : null;
        } else {
            return delegate.getChild(element);
        }
    }

    @Override
    public Resource requireChild(PathElement element) {
        if (CORE_ADDRESS.equals(element.getKey())) {
            if (hasAddressControl(element)) {
                return CoreAddressResource.INSTANCE;
            }
            throw new NoSuchResourceException(element);
        } else {
            return delegate.requireChild(element);
        }
    }

    @Override
    public boolean hasChildren(String childType) {
        if (CORE_ADDRESS.equals(childType)) {
            return getChildrenNames(CORE_ADDRESS).size() > 0;
        } else {
            return delegate.hasChildren(childType);
        }
    }

    @Override
    public Resource navigate(PathAddress address) {
        if (address.size() > 0 && CORE_ADDRESS.equals(address.getElement(0).getKey())) {
            if (address.size() > 1) {
                throw new NoSuchResourceException(address.getElement(1));
            }
            return CoreAddressResource.INSTANCE;
        } else {
            return delegate.navigate(address);
        }
    }

    @Override
    public Set<String> getChildTypes() {
        Set<String> result = new HashSet<String>(delegate.getChildTypes());
        result.add(CORE_ADDRESS);
        return result;
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (CORE_ADDRESS.equals(childType)) {
            return getCoreAddressNames();
        } else {
            return delegate.getChildrenNames(childType);
        }
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (CORE_ADDRESS.equals(childType)) {
            Set<ResourceEntry> result = new HashSet<ResourceEntry>();
            for (String name : getCoreAddressNames()) {
                result.add(new CoreAddressResource.CoreAddressResourceEntry(name));
            }
            return result;
        } else {
            return delegate.getChildren(childType);
        }
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        if (CORE_ADDRESS.equals(address.getKey())) {
            throw new UnsupportedOperationException(String.format("Resources of type %s cannot be registered", CORE_ADDRESS));
        } else {
            delegate.registerChild(address, resource);
        }
    }

    @Override
    public Resource removeChild(PathElement address) {
        if (CORE_ADDRESS.equals(address.getKey())) {
            throw new UnsupportedOperationException(String.format("Resources of type %s cannot be removed", CORE_ADDRESS));
        } else {
            return delegate.removeChild(address);
        }
    }

    @Override
    public boolean isRuntime() {
        return false;
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public Resource clone() {
        HornetQServerResource clone = new HornetQServerResource(delegate.clone());
        clone.setHornetQServerServiceController(hornetQServerServiceController);
        return clone;
    }

    private boolean hasAddressControl(PathElement element) {
        final ManagementService managementService = getManagementService();
        return managementService == null ? false : managementService.getResource(ResourceNames.CORE_ADDRESS + element.getValue()) != null;
    }

    private Set<String> getCoreAddressNames() {
        final ManagementService managementService = getManagementService();
        if (managementService == null) {
            return Collections.emptySet();
        } else {
            Set<String> result = new HashSet<String>();
            for (Object obj : managementService.getResources(AddressControl.class)) {
                AddressControl ac = AddressControl.class.cast(obj);
                result.add(ac.getAddress());
            }
            return result;
        }
    }

    private ManagementService getManagementService() {
        if (hornetQServerServiceController == null
                || hornetQServerServiceController.getState() != ServiceController.State.UP) {
            return null;
        } else {
            return hornetQServerServiceController.getValue().getManagementService();
        }
    }
}
