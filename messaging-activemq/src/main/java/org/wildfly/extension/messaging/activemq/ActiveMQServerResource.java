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

import static org.wildfly.extension.messaging.activemq.CommonAttributes.CORE_ADDRESS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.RUNTIME_QUEUE;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.activemq.artemis.api.core.management.AddressControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.management.ManagementService;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Resource representing a ActiveMQ server.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ActiveMQServerResource implements Resource {

    private final Resource delegate;
    private volatile ServiceController<ActiveMQServer> activeMQServerServiceController;

    public ActiveMQServerResource() {
        this(Resource.Factory.create());
    }

    public ActiveMQServerResource(final Resource delegate) {
        this.delegate = delegate;
    }

    public ServiceController<ActiveMQServer> getActiveMQServerServiceController() {
        return activeMQServerServiceController;
    }

    public void setActiveMQServerServiceController(ServiceController<ActiveMQServer> activeMQServerServiceController) {
        this.activeMQServerServiceController = activeMQServerServiceController;
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
        } else if (RUNTIME_QUEUE.equals(element.getKey())) {
            return hasQueueControl(element.getValue());
        } else {
            return delegate.hasChild(element);
        }
    }

    @Override
    public Resource getChild(PathElement element) {
        if (CORE_ADDRESS.equals(element.getKey())) {
            return hasAddressControl(element) ? new CoreAddressResource(element.getValue(), getManagementService()) : null;
        } else if (RUNTIME_QUEUE.equals(element.getKey())) {
            return hasQueueControl(element.getValue()) ? PlaceholderResource.INSTANCE : null;
        } else {
            return delegate.getChild(element);
        }
    }

    @Override
    public Resource requireChild(PathElement element) {
        if (CORE_ADDRESS.equals(element.getKey())) {
            if (hasAddressControl(element)) {
                return new CoreAddressResource(element.getValue(), getManagementService());
            }
            throw new NoSuchResourceException(element);
        } else if (RUNTIME_QUEUE.equals(element.getKey())) {
            if (hasQueueControl(element.getValue())) {
                return PlaceholderResource.INSTANCE;
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
        } else if (RUNTIME_QUEUE.equals(childType)) {
            return getChildrenNames(RUNTIME_QUEUE).size() > 0;
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
            return new CoreAddressResource(address.getElement(0).getValue(), getManagementService());
        } else if (address.size() > 0 && RUNTIME_QUEUE.equals(address.getElement(0).getKey())) {
            if (address.size() > 1) {
                throw new NoSuchResourceException(address.getElement(1));
            }
            return PlaceholderResource.INSTANCE;
        } else {
            return delegate.navigate(address);
        }
    }

    @Override
    public Set<String> getChildTypes() {
        Set<String> result = new HashSet<String>(delegate.getChildTypes());
        result.add(CORE_ADDRESS);
        result.add(RUNTIME_QUEUE);
        return result;
    }

    @Override
    public Set<String> getOrderedChildTypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (CORE_ADDRESS.equals(childType)) {
            return getCoreAddressNames();
        } else if (RUNTIME_QUEUE.equals(childType)) {
            return getCoreQueueNames();
        } else {
            return delegate.getChildrenNames(childType);
        }
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (CORE_ADDRESS.equals(childType)) {
            Set<ResourceEntry> result = new HashSet<ResourceEntry>();
            for (String name : getCoreAddressNames()) {
                result.add(new CoreAddressResource.CoreAddressResourceEntry(name, getManagementService()));
            }
            return result;
        } else if (RUNTIME_QUEUE.equals(childType)) {
            Set<ResourceEntry> result = new LinkedHashSet<ResourceEntry>();
            for (String name : getCoreQueueNames()) {
                result.add(new PlaceholderResource.PlaceholderResourceEntry(RUNTIME_QUEUE, name));
            }
            return result;
        } else {
            return delegate.getChildren(childType);
        }
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        String type = address.getKey();
        if (CORE_ADDRESS.equals(type) ||
                RUNTIME_QUEUE.equals(type)) {
            throw MessagingLogger.ROOT_LOGGER.canNotRegisterResourceOfType(type);
        } else {
            delegate.registerChild(address, resource);
        }
    }

    @Override
    public void registerChild(PathElement address, int index, Resource resource) {
        throw MessagingLogger.ROOT_LOGGER.indexedChildResourceRegistrationNotAvailable(address);
    }

    @Override
    public Resource removeChild(PathElement address) {
        String type = address.getKey();
        if (CORE_ADDRESS.equals(type) ||
                RUNTIME_QUEUE.equals(type)) {
            throw MessagingLogger.ROOT_LOGGER.canNotRemoveResourceOfType(type);
        } else {
            return delegate.removeChild(address);
        }
    }

    @Override
    public boolean isRuntime() {
        return delegate.isRuntime();
    }

    @Override
    public boolean isProxy() {
        return delegate.isProxy();
    }

    @Override
    public Resource clone() {
        ActiveMQServerResource clone = new ActiveMQServerResource(delegate.clone());
        clone.setActiveMQServerServiceController(activeMQServerServiceController);
        return clone;
    }

    private boolean hasAddressControl(PathElement element) {
        final ManagementService managementService = getManagementService();
        return managementService == null ? false : managementService.getResource(ResourceNames.ADDRESS + element.getValue()) != null;
    }

    private boolean hasQueueControl(String name) {
        final ManagementService managementService = getManagementService();
        // for backwards compatibility, if the queue name starts with "jms.queue." (as in Artemis 1.x),
        // we strip it to get only the actual name (as in Artemis 2.x)
        if (name.startsWith("jms.queue.")) {
            name = name.substring("jms.queue.".length());
        }
        return managementService == null ? false : managementService.getResource(ResourceNames.QUEUE + name) != null;
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

    private Set<String> getCoreQueueNames() {
        final ManagementService managementService = getManagementService();
        if (managementService == null) {
            return Collections.emptySet();
        } else {
            Set<String> result = new HashSet<String>();
            for (Object obj : managementService.getResources(QueueControl.class)) {
                QueueControl qc = QueueControl.class.cast(obj);
                result.add(qc.getName());
            }
            return result;
        }
    }

    private ManagementService getManagementService() {
        if (activeMQServerServiceController == null
                || activeMQServerServiceController.getState() != ServiceController.State.UP) {
            return null;
        } else {
            return activeMQServerServiceController.getValue().getManagementService();
        }
    }
}
