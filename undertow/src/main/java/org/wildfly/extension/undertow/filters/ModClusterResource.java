/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.filters;

import io.undertow.server.handlers.proxy.mod_cluster.ModCluster;
import io.undertow.server.handlers.proxy.mod_cluster.ModClusterController;
import io.undertow.server.handlers.proxy.mod_cluster.ModClusterStatus;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.wildfly.extension.undertow.Constants.BALANCER;


public class ModClusterResource implements Resource {

    private final Resource delegate;
    private final String name;

    public ModClusterResource(Resource delegate, String name) {
        this.delegate = delegate;
        this.name = name;
    }

    @Override
    public ModelNode getModel() {
        return delegate.getModel();
    }

    @Override
    public void writeModel(final ModelNode newModel) {
        delegate.writeModel(newModel);
    }

    @Override
    public boolean isModelDefined() {
        return delegate.isModelDefined();
    }

    @Override
    public boolean hasChild(final PathElement element) {
        if (BALANCER.equals(element.getKey())) {
            return getChildrenNames(BALANCER).contains(element.getValue());
        }
        return delegate.hasChild(element);
    }

    @Override
    public Resource getChild(final PathElement element) {
        if (BALANCER.equals(element.getKey())) {
            if (getChildrenNames(BALANCER).contains(element.getValue())) {
                return new ModClusterBalancerResource(element.getValue(), name);
            }
            return null;
        }
        return delegate.getChild(element);
    }

    @Override
    public Resource requireChild(final PathElement element) {
        if (BALANCER.equals(element.getKey())) {
            if (getChildrenNames(BALANCER).contains(element.getValue())) {
                return new ModClusterBalancerResource(element.getValue(), name);
            }
            throw new NoSuchResourceException(element);
        }
        return delegate.requireChild(element);
    }

    @Override
    public boolean hasChildren(final String childType) {
        if (BALANCER.equals(childType)) {
            return !getChildrenNames(BALANCER).isEmpty();
        }
        return delegate.hasChildren(childType);
    }

    @Override
    public Resource navigate(final PathAddress address) {
        if (address.size() > 0 && BALANCER.equals(address.getElement(0).getKey())) {
            final Resource modClusterBalancerResource = requireChild(address.getElement(0));
            if(address.size() == 1) {
                return modClusterBalancerResource;
            } else {
                return modClusterBalancerResource.navigate(address.subAddress(1));
            }
        }
        return delegate.navigate(address);
    }

    @Override
    public Set<String> getChildTypes() {
        final Set<String> result = new LinkedHashSet<>(delegate.getChildTypes());
        result.add(BALANCER);
        return result;
    }

    @Override
    public Set<String> getChildrenNames(final String childType) {
        if (BALANCER.equals(childType)) {

            ModClusterService service = service(name);
            if(service == null) {
                return Collections.emptySet();
            }
            ModCluster modCluster = service.getModCluster();
            if(modCluster == null) {
                return Collections.emptySet();
            }
            ModClusterController controller = modCluster.getController();
            if(controller == null) {
                return Collections.emptySet();
            }
            ModClusterStatus status = controller.getStatus();
            final Set<String> result = new LinkedHashSet<>();
            for (ModClusterStatus.LoadBalancer balancer : status.getLoadBalancers()) {
                result.add(balancer.getName());
            }
            return result;
        }
        return delegate.getChildrenNames(childType);
    }

    @Override
    public Set<ResourceEntry> getChildren(final String childType) {
        if (BALANCER.equals(childType)) {
            final Set<String> names = getChildrenNames(childType);
            final Set<ResourceEntry> result = new LinkedHashSet<>(names.size());
            for (String name : names) {
                result.add(new ModClusterBalancerResource(name, this.name));
            }
            return result;
        }
        return delegate.getChildren(childType);
    }

    @Override
    public void registerChild(final PathElement address, final Resource resource) {
        final String type = address.getKey();
        if (BALANCER.equals(type)) {
            throw UndertowLogger.ROOT_LOGGER.cannotRegisterResourceOfType(type);
        }
        delegate.registerChild(address, resource);
    }

    @Override
    public void registerChild(PathElement address, int index, Resource resource) {
        final String type = address.getKey();
        if (BALANCER.equals(type)) {
            throw UndertowLogger.ROOT_LOGGER.cannotRegisterResourceOfType(type);
        }
        delegate.registerChild(address, index, resource);
    }

    @Override
    public Resource removeChild(final PathElement address) {
        final String type = address.getKey();
        if (BALANCER.equals(type)) {
            throw UndertowLogger.ROOT_LOGGER.cannotRemoveResourceOfType(type);
        }
        return delegate.removeChild(address);
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
    public Set<String> getOrderedChildTypes() {
        return Collections.emptySet();
    }

    @Override
    public Resource clone() {
        return new ModClusterResource(delegate.clone(), name);
    }

    static ModClusterService service(String name) {
        final ServiceContainer serviceContainer = CurrentServiceContainer.getServiceContainer();
        if(serviceContainer == null) {
            //for tests
            return null;
        }
        ServiceController<?> cluster = serviceContainer.getService(UndertowService.FILTER.append(name));
        if(cluster == null) {
            return null;
        }
        return (ModClusterService) cluster.getService();
    }
}
