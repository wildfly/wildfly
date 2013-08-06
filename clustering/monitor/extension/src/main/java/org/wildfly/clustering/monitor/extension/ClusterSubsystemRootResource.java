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
package org.wildfly.clustering.monitor.extension;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

// import org.jboss.as.clustering.jgroups.subsystem.ChannelInstanceResource;

/**
 * Custom resource to allow dynamic detection of cluster resources.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusterSubsystemRootResource implements Resource {

    private final Resource delegate;
    private volatile ServiceRegistry registry;

    public ClusterSubsystemRootResource() {
        this(Factory.create());
    }

    public ClusterSubsystemRootResource(final Resource delegate) {
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
        if (ModelKeys.CLUSTER.equals(element.getKey())) {
            return hasChannel(element);
        } else {
            return delegate.hasChild(element);
        }
    }

    @Override
    public Resource getChild(PathElement element) {
        if (ModelKeys.CLUSTER.equals(element.getKey())) {
            if (hasChannel(element)) {
                String cluster = element.getValue();

                ClusterInstanceResource resource = new ClusterInstanceResource(cluster);
                resource.setRegistry(this.registry);
                return resource;
                // return PlaceholderResource.INSTANCE;
            } else {
                return null;
            }
        } else {
            return delegate.getChild(element);
        }
    }

    @Override
    public Resource requireChild(PathElement element) {
        if (ModelKeys.CLUSTER.equals(element.getKey())) {
            if (hasChannel(element)) {
                String cluster = element.getValue();

                ClusterInstanceResource resource = new ClusterInstanceResource(cluster);
                resource.setRegistry(this.registry);
                return resource;
                //return PlaceholderResource.INSTANCE;
            }
            throw new NoSuchResourceException(element);
        } else {
            return delegate.requireChild(element);
        }
    }

    @Override
    public boolean hasChildren(String childType) {
        if (ModelKeys.CLUSTER.equals(childType)) {
            return getChildrenNames(ModelKeys.CLUSTER).size() > 0;
        } else {
            return delegate.hasChildren(childType);
        }
    }

    @Override
    public Resource navigate(PathAddress address) {
        if (address.size() > 0 && ModelKeys.CLUSTER.equals(address.getElement(0).getKey())) {
            // resource too deep
            if (address.size() > 1) {
                throw new NoSuchResourceException(address.getElement(1));
            }
            // resource doesn't exist
            if (!hasChannel(address.getElement(0))) {
                throw new NoSuchResourceException(address.getElement(0));
            }
            String name = address.getElement(0).getValue();

            ClusterInstanceResource resource = new ClusterInstanceResource(name);
            resource.setRegistry(this.registry);
            return resource;
            // return PlaceholderResource.INSTANCE;
        } else {
            return delegate.navigate(address);
        }
    }

    @Override
    public Set<String> getChildTypes() {
        Set<String> result = new HashSet<String>(delegate.getChildTypes());
        result.add(ModelKeys.CLUSTER);
        return result;
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (ModelKeys.CLUSTER.equals(childType)) {
            return ClusterSubsystemHelper.getChannelNames(this.registry);
        } else {
            return delegate.getChildrenNames(childType);
        }
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (ModelKeys.CLUSTER.equals(childType)) {
            Set<ResourceEntry> result = new HashSet<ResourceEntry>();
            for (ServiceName serviceName : ClusterSubsystemHelper.getChannelServiceNames(this.registry)) {
                // build up a set of ResourceEntry descriptions
                String channel = ClusterSubsystemHelper.getChannelNameFromChannelServiceName(serviceName);

                ClusterInstanceResource.ClusterInstanceResourceEntry entry =
                        new ClusterInstanceResource.ClusterInstanceResourceEntry(childType, channel);
                entry.setRegistry(this.registry);

                result.add(entry) ;
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
        if (ModelKeys.CLUSTER.equals(type)) {
            // throw an exception here indicating we cannot register resource of type X
        } else {
            delegate.registerChild(address, resource);
        }
    }

    @Override
    public Resource removeChild(PathElement address) {
        String type = address.getKey();
        if (ModelKeys.CLUSTER.equals(type)) {
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
        ClusterSubsystemRootResource clone = new ClusterSubsystemRootResource(delegate.clone());
        // set the pointer to the ServiceRegistry
        clone.setRegistry(this.getRegistry());
        return clone;
    }

    /*
     * Returns true if a Channel exists at this address (channel=X) and is in UP state.
     */
    private boolean hasChannel(PathElement element) {
        if (registry == null) return false;
        assert element.getKey().equals(ModelKeys.CLUSTER);
        ServiceName channelName = ClusterSubsystemHelper.CHANNEL_PARENT.append(element.getValue());
        ServiceController<?> controller = registry.getService(channelName);
        return (controller != null) && ClusterSubsystemHelper.serviceIsUp(this.registry, channelName);
    }

}
