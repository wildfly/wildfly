/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.CORE_ADDRESS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.RUNTIME_QUEUE;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * Resource representing a ActiveMQ server.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ActiveMQServerResource implements Resource {

    private final Resource delegate;
    private volatile ServiceController<ActiveMQBroker> activeMQServerServiceController;

    public ActiveMQServerResource() {
        this(Factory.create());
    }

    public ActiveMQServerResource(final Resource delegate) {
        this.delegate = delegate;
    }

    public ServiceController<ActiveMQBroker> getActiveMQServerServiceController() {
        return activeMQServerServiceController;
    }

    public void setActiveMQServerServiceController(ServiceController<ActiveMQBroker> activeMQServerServiceController) {
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
        if (null == element.getKey()) {
            return delegate.hasChild(element);
        }
        switch (element.getKey()) {
            case CORE_ADDRESS:
                return hasAddressControl(element);
            case RUNTIME_QUEUE:
                return hasQueueControl(element.getValue());
            default:
                return delegate.hasChild(element);
        }
    }

    @Override
    public Resource getChild(PathElement element) {
        if (null == element.getKey()) {
            return delegate.getChild(element);
        }
        switch (element.getKey()) {
            case CORE_ADDRESS:
                return hasAddressControl(element) ? new CoreAddressResource(element.getValue(), getActiveMQBroker()) : null;
            case RUNTIME_QUEUE:
                return hasQueueControl(element.getValue()) ? PlaceholderResource.INSTANCE : null;
            default:
                return delegate.getChild(element);
        }
    }

    @Override
    public Resource requireChild(PathElement element) {
        if (null == element.getKey()) {
            return delegate.requireChild(element);
        }
        switch (element.getKey()) {
            case CORE_ADDRESS:
                if (hasAddressControl(element)) {
                    return new CoreAddressResource(element.getValue(), getActiveMQBroker());
                }
                throw new NoSuchResourceException(element);
            case RUNTIME_QUEUE:
                if (hasQueueControl(element.getValue())) {
                    return PlaceholderResource.INSTANCE;
                }
                throw new NoSuchResourceException(element);
            default:
                return delegate.requireChild(element);
        }
    }

    @Override
    public boolean hasChildren(String childType) {
        if (null == childType) {
            return delegate.hasChildren(childType);
        }
        switch (childType) {
            case CORE_ADDRESS:
                return !getChildrenNames(CORE_ADDRESS).isEmpty();
            case RUNTIME_QUEUE:
                return !getChildrenNames(RUNTIME_QUEUE).isEmpty();
            default:
                return delegate.hasChildren(childType);
        }
    }

    @Override
    public Resource navigate(PathAddress address) {
        if (address.size() > 0 && CORE_ADDRESS.equals(address.getElement(0).getKey())) {
            if (address.size() > 1) {
                throw new NoSuchResourceException(address.getElement(1));
            }
            return new CoreAddressResource(address.getElement(0).getValue(), getActiveMQBroker());
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
        Set<String> result = new HashSet<>(delegate.getChildTypes());
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
        if (null == childType) {
            return delegate.getChildrenNames(childType);
        }
        switch (childType) {
            case CORE_ADDRESS:
                return getCoreAddressNames();
            case RUNTIME_QUEUE:
                return getCoreQueueNames();
            default:
                return delegate.getChildrenNames(childType);
        }
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (null == childType) {
            return delegate.getChildren(childType);
        }
        switch (childType) {
            case CORE_ADDRESS: {
                Set<ResourceEntry> result = new HashSet<>();
                for (String name : getCoreAddressNames()) {
                    result.add(new CoreAddressResource.CoreAddressResourceEntry(name, getActiveMQBroker()));
                }
                return result;
            }
            case RUNTIME_QUEUE: {
                Set<ResourceEntry> result = new LinkedHashSet<ResourceEntry>();
                for (String name : getCoreQueueNames()) {
                    result.add(new PlaceholderResource.PlaceholderResourceEntry(RUNTIME_QUEUE, name));
                }
                return result;
            }
            default:
                return delegate.getChildren(childType);
        }
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        String type = address.getKey();
        if (CORE_ADDRESS.equals(type)
                || RUNTIME_QUEUE.equals(type)) {
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
        if (CORE_ADDRESS.equals(type)
                || RUNTIME_QUEUE.equals(type)) {
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
        final ActiveMQBroker broker = getActiveMQBroker();
        return broker == null ? false : broker.getResource(ResourceNames.ADDRESS + element.getValue()) != null;
    }

    private boolean hasQueueControl(String name) {
        final ActiveMQBroker broker = getActiveMQBroker();
        return broker == null ? false : broker.getResource(ResourceNames.QUEUE + name) != null;
    }

    private Set<String> getCoreAddressNames() {
        final ActiveMQBroker broker = getActiveMQBroker();
        if (broker == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(broker.getCoreAddressNames());
    }

    private Set<String> getCoreQueueNames() {
        final ActiveMQBroker broker = getActiveMQBroker();
        if (broker == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(broker.getQueueControlNames());
    }

    private ActiveMQBroker getActiveMQBroker() {
        if (activeMQServerServiceController == null
                || activeMQServerServiceController.getState() != ServiceController.State.UP) {
            return null;
        } else {
            return activeMQServerServiceController.getValue();
        }
    }
}
