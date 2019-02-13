/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.singleton;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.DelegatingResource;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.Services;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;

/**
 * Custom resource that additionally reports runtime singleton deployments and services.
 * @author Paul Ferraro
 */
public class SingletonPolicyResource extends DelegatingResource implements Registrar<ServiceName> {

    private static final String DEPLOYMENT_CHILD_TYPE = SingletonDeploymentResourceDefinition.WILDCARD_PATH.getKey();
    private static final String SERVICE_CHILD_TYPE = SingletonServiceResourceDefinition.WILDCARD_PATH.getKey();

    final Set<ServiceName> services;
    final Set<String> deployments;

    public SingletonPolicyResource(Resource resource) {
        this(resource, ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet());
    }

    private SingletonPolicyResource(Resource resource, Set<ServiceName> services, Set<String> deployments) {
        super(resource);
        this.services = services;
        this.deployments = deployments;
    }

    @Override
    public Registration register(ServiceName service) {
        if (Services.JBOSS_DEPLOYMENT.isParentOf(service)) {
            String deploymentName = SingletonDeploymentResourceDefinition.pathElement(service).getValue();
            this.deployments.add(deploymentName);
            return new Registration() {
                @Override
                public void close() {
                    SingletonPolicyResource.this.deployments.remove(deploymentName);
                }
            };
        }
        this.services.add(service);
        return new Registration() {
            @Override
            public void close() {
                SingletonPolicyResource.this.services.remove(service);
            }
        };
    }

    @Override
    public Resource clone() {
        return new SingletonPolicyResource(super.clone(), this.services, this.deployments);
    }

    @Override
    public Resource getChild(PathElement path) {
        String childType = path.getKey();
        if (childType.equals(DEPLOYMENT_CHILD_TYPE)) {
            return this.deployments.contains(path.getValue()) ? PlaceholderResource.INSTANCE : null;
        }
        if (childType.equals(SERVICE_CHILD_TYPE)) {
            return this.services.contains(ServiceName.parse(path.getValue())) ? PlaceholderResource.INSTANCE : null;
        }
        return super.getChild(path);
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (childType.equals(DEPLOYMENT_CHILD_TYPE)) {
            Set<ResourceEntry> entries = new HashSet<>();
            for (String deployment : this.deployments) {
                entries.add(new PlaceholderResource.PlaceholderResourceEntry(SingletonDeploymentResourceDefinition.pathElement(deployment)));
            }
            return entries;
        }
        if (childType.equals(SERVICE_CHILD_TYPE)) {
            Set<ResourceEntry> entries = new HashSet<>();
            for (ServiceName service : this.services) {
                entries.add(new PlaceholderResource.PlaceholderResourceEntry(SingletonServiceResourceDefinition.pathElement(service)));
            }
            return entries;
        }
        return super.getChildren(childType);
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (childType.equals(DEPLOYMENT_CHILD_TYPE)) {
            return this.deployments;
        }
        if (childType.equals(SERVICE_CHILD_TYPE)) {
            Set<String> names = new HashSet<>();
            for (ServiceName service : this.services) {
                names.add(SingletonServiceResourceDefinition.pathElement(service).getValue());
            }
            return names;
        }
        return super.getChildrenNames(childType);
    }

    @Override
    public Set<String> getChildTypes() {
        Set<String> childTypes = new HashSet<>(super.getChildTypes());
        childTypes.add(DEPLOYMENT_CHILD_TYPE);
        childTypes.add(SERVICE_CHILD_TYPE);
        return childTypes;
    }

    @Override
    public boolean hasChild(PathElement path) {
        String childType = path.getKey();
        if (childType.equals(DEPLOYMENT_CHILD_TYPE)) {
            return this.deployments.contains(path.getValue());
        }
        if (childType.equals(SERVICE_CHILD_TYPE)) {
            return this.services.contains(ServiceName.parse(path.getValue()));
        }
        return super.hasChild(path);
    }

    @Override
    public boolean hasChildren(String childType) {
        if (childType.equals(DEPLOYMENT_CHILD_TYPE)) {
            return !this.deployments.isEmpty();
        }
        if (childType.equals(SERVICE_CHILD_TYPE)) {
            return !this.services.isEmpty();
        }
        return super.hasChildren(childType);
    }

    @Override
    public Resource navigate(PathAddress address) {
        return (address.size() == 1) ? this.requireChild(address.getLastElement()) : super.navigate(address);
    }

    @Override
    public Resource requireChild(PathElement path) {
        Resource resource = this.getChild(path);
        if (resource == null) {
            throw new NoSuchResourceException(path);
        }
        return resource;
    }
}
