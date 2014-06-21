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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.MetricKeys.CHANNEL;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jgroups.Channel;

/**
 * Custom resource to allow dynamic detection of channel resources.
 *
 * Custom resources are concerned with the following:
 * - the model used for storing persistent attributes
 * - the children of the resource
 * In this case, the subsystem root resource is modified to allow the
 * use of a custom ChannelInstance resource to represent JGroups channel metrics
 * via run-time only resources.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class JGroupsSubsystemResource implements Resource {

    private final Resource delegate;
    private volatile ServiceRegistry registry;

    public JGroupsSubsystemResource() {
        this(Resource.Factory.create());
    }

    public JGroupsSubsystemResource(final Resource delegate) {
        this.delegate = delegate;
    }

    public ServiceRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(ServiceRegistry registry) {
        this.registry = registry;
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
        if (CHANNEL.equals(element.getKey())) {
            return hasChannel(element);
        } else {
            return delegate.hasChild(element);
        }
    }

    @Override
    public Resource getChild(PathElement element) {
        if (CHANNEL.equals(element.getKey())) {
            if (hasChannel(element)) {
                String name = element.getValue();
                ServiceName serviceName = getServiceNameFromName(name);
                ServiceController<Channel> serviceController = ServiceContainerHelper.getService(this.registry, serviceName);
                return new ChannelInstanceResource(serviceController);
                //return PlaceholderResource.INSTANCE;
            } else {
                return null;
            }
        } else {
            return delegate.getChild(element);
        }
    }

    @Override
    public Resource requireChild(PathElement element) {
        if (CHANNEL.equals(element.getKey())) {
            if (hasChannel(element)) {
                String name = element.getValue();
                ServiceName serviceName = getServiceNameFromName(name);
                ServiceController<Channel> serviceController = ServiceContainerHelper.getService(this.registry, serviceName);
                return new ChannelInstanceResource(serviceController);
                // return PlaceholderResource.INSTANCE;
            }
            throw new NoSuchResourceException(element);
        } else {
            return delegate.requireChild(element);
        }
    }

    @Override
    public boolean hasChildren(String childType) {
        if (CHANNEL.equals(childType)) {
            return getChildrenNames(MetricKeys.CHANNEL).size() > 0;
        } else {
            return delegate.hasChildren(childType);
        }
    }

    @Override
    public Resource navigate(PathAddress address) {
        if (address.size() > 0 && MetricKeys.CHANNEL.equals(address.getElement(0).getKey())) {
            // resource too deep
            if (address.size() > 1) {
                throw new NoSuchResourceException(address.getElement(1));
            }
            // resource doesn't exist
            if (!hasChannel(address.getElement(0))) {
                throw new NoSuchResourceException(address.getElement(0));
            }
            String name = address.getElement(0).getValue();
            ServiceName serviceName = getServiceNameFromName(name);
            ServiceController<Channel> serviceController = ServiceContainerHelper.getService(this.registry, serviceName);
            return new ChannelInstanceResource(serviceController);
            //return PlaceholderResource.INSTANCE;
        } else {
            return delegate.navigate(address);
        }
    }

    @Override
    public Set<String> getChildTypes() {
        Set<String> result = new HashSet<String>(delegate.getChildTypes());
        result.add(CHANNEL);
        return result;
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (CHANNEL.equals(childType)) {
            return getChannelNames();
        } else {
            return delegate.getChildrenNames(childType);
        }
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (CHANNEL.equals(childType)) {
            Set<ResourceEntry> result = new HashSet<ResourceEntry>();
            for (ServiceName serviceName : getChannelServiceNames()) {
                // build up a set of ResourceEntry descriptions
                ServiceController<Channel> serviceController = ServiceContainerHelper.getService(this.registry, serviceName);
                String name = getNameFromServiceName(serviceName);
                result.add(new ChannelInstanceResource.ChannelInstanceResourceEntry(serviceController, MetricKeys.CHANNEL, name));
                //result.add(new PlaceholderResource.PlaceholderResourceEntry(childType, name));
            }
            return result;
        } else {
            return delegate.getChildren(childType);
        }
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        String type = address.getKey();
        if (CHANNEL.equals(type)) {
            // throw an exception here indicating we cannot register resource of type X
        } else {
            delegate.registerChild(address, resource);
        }
    }

    @Override
    public Resource removeChild(PathElement address) {
        String type = address.getKey();
        if (CHANNEL.equals(type)) {
            // throw an exception here indicating we cannot remove resource of type X
            return null;
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
        JGroupsSubsystemResource clone = new JGroupsSubsystemResource(delegate.clone());
        // set the pointer to the ServiceRegistry
        clone.setRegistry(this.getRegistry());
        return clone;
    }

    /*
     * Returns true if a Channel exists at this address (channel=X) and is in UP state.
     */
    private boolean hasChannel(PathElement element) {
        if (registry == null) return false;
        assert element.getKey().equals(MetricKeys.CHANNEL);
        ServiceName channelName = ChannelInstanceResource.CHANNEL_PARENT.append(element.getValue());
        ServiceController<?> controller = registry.getService(channelName);
        return (controller != null) && serviceIsUp(channelName);
    }

    /*
     * Returns the set of all Channel ServiceNames at this address (service jboss.jgroups.channel.*)
     * which are in the UP state.
     */
    private Set<ServiceName> getChannelServiceNames() {
        if (registry == null) {
            return Collections.emptySet();
        }
        Set<ServiceName> channelServiceNames = new HashSet<ServiceName>();
        assert registry != null;
        List<ServiceName> serviceNames = registry.getServiceNames();
        for (ServiceName serviceName : serviceNames) {
            if (ChannelInstanceResource.CHANNEL_PARENT.isParentOf(serviceName)) {
                if (serviceIsUp(serviceName)) {
                    channelServiceNames.add(serviceName);
                }
            }
        }
        return channelServiceNames;
    }

    /*
     * Returns the set of all Channel names at this address (channel=*)
     * which are in the UP state.
     */
    private Set<String> getChannelNames() {
        Set<String> channelNames = new HashSet<String>();
        for (ServiceName serviceName : getChannelServiceNames()) {
            if (serviceIsUp(serviceName)) {
               channelNames.add(getNameFromServiceName(serviceName));
            }
        }
        return channelNames;
    }

    private String getNameFromServiceName(ServiceName serviceName) {
        String serviceNameAsString = serviceName.toString();
        return serviceNameAsString.substring(ChannelInstanceResource.CHANNEL_PREFIX_LENGTH + 1);
    }

    private ServiceName getServiceNameFromName(String name) {
        return ChannelInstanceResource.CHANNEL_PARENT.append(name);
    }

    /*
     * We want to return the names only of channel controllers which are UP
     * This method may be called with service names which do not correspond to existing channel services!
     */
    private boolean serviceIsUp(ServiceName name) {
        if (registry == null) return false;
        ServiceController<?> controller = registry.getService(name);
        return (controller != null) ? ServiceController.State.UP.equals(controller.getState()) : false;
    }
}
