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

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Custom resource to allow dynamic detection, registration and return of deployment resources.
 * These include:
 * - distributable web deployments
 * - clustered SFSB bean deployments
 *
 * This resource can only have web=WEB and bean=* deployments, so we do not need a delegate.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class DeploymentInstanceResource implements Resource {

    private final String name ;
    private volatile String channel ;
    private volatile ServiceRegistry registry;

    public DeploymentInstanceResource(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public ServiceRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(ServiceRegistry registry) {
        this.registry = registry;
    }

    // this resource holds no persistent state, so "turn off" the model
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

    // this resource does have children, so activate te creation of children
    // the children are protocol resources associated with the channel
    @Override
    public boolean hasChild(PathElement element) {
        if (ModelKeys.WEB.equals(element.getKey())) {
            return hasWebDeployment(element);
        }
        if (ModelKeys.BEAN.equals(element.getKey())) {
            return hasBeanDeployment(element);
        }
        return false;
    }

    @Override
    public Resource requireChild(PathElement element) {
        if (ModelKeys.WEB.equals(element.getKey())) {
            if (hasWebDeployment(element)) {
                // this should be OK
                return PlaceholderResource.INSTANCE;
            }
        }
        if (ModelKeys.BEAN.equals(element.getKey())) {
            if (hasBeanDeployment(element)) {
                // this should be OK
                return PlaceholderResource.INSTANCE;
            }
        }
        throw new NoSuchResourceException(element);
    }

    @Override
    public Resource getChild(PathElement element) {
        if (ModelKeys.WEB.equals(element.getKey())) {
            if (hasWebDeployment(element)) {
                return PlaceholderResource.INSTANCE;
            }
        }
        if (ModelKeys.BEAN.equals(element.getKey())) {
            if (hasBeanDeployment(element)) {
                return PlaceholderResource.INSTANCE;
            }
        }
        return null;
    }

    @Override
    public Resource removeChild(PathElement address) {
        throw MESSAGES.immutableResource();
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        throw MESSAGES.immutableResource();
    }

    @Override
    public boolean hasChildren(String childType) {
        if (ModelKeys.WEB.equals(childType)) {
            return getChildrenNames(ModelKeys.WEB).size() > 0;
        }
        if (ModelKeys.BEAN.equals(childType)) {
            return getChildrenNames(ModelKeys.BEAN).size() > 0;
        }
        return false;
    }

    /*
     * Returns the child resources for a deployment. We have:
     * - a child resource web=WEB if this deployment is in the set of web deployments for the cluster
     * - a child resource bean=X if this deployment contains such a @Clustered bean
     */
    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (ModelKeys.WEB.equals(childType)) {
            Set<ResourceEntry> result = new HashSet<ResourceEntry>();
            // we have a child resource web=WEB if this deployment is in the set of web deployments for the cluster
            if (ClusterSubsystemHelper.getWebDeploymentNames(this.registry, this.channel).contains(name)) {
                result.add(new PlaceholderResource.PlaceholderResourceEntry(ModelKeys.WEB, ModelKeys.WEB_NAME));
            }
            return result;
        }
        if (ModelKeys.BEAN.equals(childType)) {
            Set<ResourceEntry> result = new HashSet<ResourceEntry>();
            for (String beanName : ClusterSubsystemHelper.getBeanNamesForDeployment(this.registry, this.channel, this.name)) {
                result.add(new PlaceholderResource.PlaceholderResourceEntry(ModelKeys.BEAN, beanName));
            }
            return result;
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (ModelKeys.WEB.equals(childType)) {
            if (ClusterSubsystemHelper.getWebDeploymentNames(this.registry, this.channel).contains(name)) {
                HashSet<String> names = new HashSet<String>();
                names.add(ModelKeys.WEB_NAME);
                return names;
            }
        }
        if (ModelKeys.BEAN.equals(childType)) {
            return ClusterSubsystemHelper.getBeanNamesForDeployment(this.registry, this.channel, this.name);
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> getChildTypes() {
        Set<String> result = new HashSet<String>();
        result.add(ModelKeys.WEB);
        result.add(ModelKeys.BEAN);
        return result;
    }

    @Override
    public Resource navigate(PathAddress address) {
        // TODO: check this
        return Tools.navigate(this, address);
    }

    @Override
    public Resource clone() {
        DeploymentInstanceResource resource = new DeploymentInstanceResource(this.name);
        // set the pointer to the ServiceRegistry
        resource.setRegistry(this.registry);
        resource.setChannel(this.channel);
        return resource;
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    /*
     * The path elements here are ("web", "WEB") and ("bean", *), so:
     *  - it has a web deployment if this.name is in the set of web deployments
     *  - if has a bean deployment of the bean name is in the set of bean deployments
     */
    private boolean hasWebDeployment(PathElement element) {
        // this should be WEB
        String deploymentName = element.getValue();
        return ClusterSubsystemHelper.getWebDeploymentNames(this.registry, this.channel).contains(this.name);
    }

    private boolean hasBeanDeployment(PathElement element) {
        String beanName = element.getValue();
        return ClusterSubsystemHelper.getBeanNamesForDeployment(this.registry, this.channel, this.name).contains(beanName);
    }

    /*
     * ResourceEntry extends the resource and additionally provides information on its path
     */
    public static class DeploymentInstanceResourceEntry extends DeploymentInstanceResource implements ResourceEntry {

        final PathElement path;

        public DeploymentInstanceResourceEntry(final PathElement path) {
            super(path.getValue());
            this.path = path;
        }

        public DeploymentInstanceResourceEntry(final String type, final String name) {
            super(name);
            this.path = PathElement.pathElement(type, name);
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
        public DeploymentInstanceResourceEntry clone() {
            return new DeploymentInstanceResourceEntry(getPathElement());
        }
    }
}
