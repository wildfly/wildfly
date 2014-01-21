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
package org.wildfly.clustering.diagnostics.extension;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Custom resource to allow dynamic detection, registration and return of deployment resources.
 * Deployments must be relative to the cluster in order to be considered.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusterInstanceResource implements Resource {

    private final String name ;
    private volatile ServiceRegistry registry;

    public ClusterInstanceResource(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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
        if (ModelKeys.DEPLOYMENT.equals(element.getKey())) {
            return hasDeployment(element);
        }
        return false;
    }

    @Override
    public Resource requireChild(PathElement element) {
        if (ModelKeys.DEPLOYMENT.equals(element.getKey())) {
            if (hasDeployment(element)) {
                // need to create a deployment resource
                String deployment = element.getValue();

                DeploymentInstanceResource resource = new DeploymentInstanceResource(deployment);
                resource.setRegistry(this.registry);
                resource.setChannel(this.name);
                return resource;
            }
        }
        throw new NoSuchResourceException(element);
    }

    @Override
    public Resource getChild(PathElement element) {
        if (ModelKeys.DEPLOYMENT.equals(element.getKey())) {
            if (hasDeployment(element)) {
                // need to create a deployment resource
                String deployment = element.getValue();

                DeploymentInstanceResource resource = new DeploymentInstanceResource(deployment);
                resource.setRegistry(this.registry);
                resource.setChannel(this.name);
                return resource;
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
        if (ModelKeys.DEPLOYMENT.equals(childType)) {
            return getChildrenNames(ModelKeys.DEPLOYMENT).size() > 0;
        } else {
            return false;
        }
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (ModelKeys.DEPLOYMENT.equals(childType)) {
            Set<ResourceEntry> result = new HashSet<ResourceEntry>();
            for (String name : ClusteringDiagnosticsSubsystemHelper.getDeploymentNames(this.registry, this.name)) {
                result.add(new DeploymentInstanceResource.DeploymentInstanceResourceEntry(ModelKeys.DEPLOYMENT, name));
            }
            return result;
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (ModelKeys.DEPLOYMENT.equals(childType)) {
            return ClusteringDiagnosticsSubsystemHelper.getDeploymentNames(this.registry, this.name);
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Set<String> getChildTypes() {
        Set<String> result = new HashSet<String>();
        result.add(ModelKeys.DEPLOYMENT);
        return result;
    }

    @Override
    public Resource navigate(PathAddress address) {
        // TODO: check this
        return Tools.navigate(this, address);
    }

    @Override
    public Resource clone() {
        ClusterInstanceResource resource = new ClusterInstanceResource(this.name);
        // set the pointer to the ServiceRegistry
        resource.setRegistry(this.registry);
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
     * THis method determines if there is a web or bean deployment which uses this channel.
     */
    private boolean hasDeployment(PathElement element) {
        String deploymentName = element.getValue();
        return ClusteringDiagnosticsSubsystemHelper.getDeploymentNames(this.registry, this.name).contains(deploymentName);
    }

    /*
     * ResourceEntry extends the resource and additionally provides information on its path
     */
    public static class ClusterInstanceResourceEntry extends ClusterInstanceResource implements ResourceEntry {

        final PathElement path;

        public ClusterInstanceResourceEntry(final PathElement path) {
            super(path.getValue());
            this.path = path;
        }

        public ClusterInstanceResourceEntry(final String type, final String name) {
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
        public ClusterInstanceResourceEntry clone() {
            return new ClusterInstanceResourceEntry(getPathElement());
        }
    }
}
