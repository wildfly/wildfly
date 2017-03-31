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
import io.undertow.server.handlers.proxy.mod_cluster.ModClusterStatus;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.wildfly.extension.undertow.Constants.CONTEXT;

public class ModClusterNodeResource implements Resource.ResourceEntry {

    private final String name;
    private final String balancerName;
    private final String modClusterName;
    private ModelNode model = new ModelNode();

    public ModClusterNodeResource(String name, String balancerName, String modClusterName) {
        this.name = name;
        this.balancerName = balancerName;
        this.modClusterName = modClusterName;
    }

    @Override
    public ModelNode getModel() {
        return model;
    }

    @Override
    public void writeModel(final ModelNode newModel) {
        this.model = newModel;
    }

    @Override
    public boolean isModelDefined() {
        return true;
    }

    @Override
    public boolean hasChild(final PathElement element) {
        if (CONTEXT.equals(element.getKey())) {
            return getChildrenNames(CONTEXT).contains(element.getValue());
        }
        return false;
    }

    @Override
    public Resource getChild(final PathElement element) {
        if (CONTEXT.equals(element.getKey())) {
            if (getChildrenNames(CONTEXT).contains(element.getValue())) {
                return PlaceholderResource.INSTANCE;
            }
            return null;
        }
        return null;
    }

    @Override
    public Resource requireChild(final PathElement element) {
        if (CONTEXT.equals(element.getKey())) {
            if (getChildrenNames(CONTEXT).contains(element.getValue())) {
                return PlaceholderResource.INSTANCE;
            }
            throw new NoSuchResourceException(element);
        }
        throw new NoSuchResourceException(element);
    }

    @Override
    public boolean hasChildren(final String childType) {
        if (CONTEXT.equals(childType)) {
            return !getChildrenNames(CONTEXT).isEmpty();
        }
        return false;
    }

    @Override
    public Resource navigate(final PathAddress address) {
        if (address.size() == 1 && CONTEXT.equals(address.getElement(0).getKey())) {
            return requireChild(address.getElement(0));
        }
        throw new NoSuchResourceException(address.getElement(0));
    }

    @Override
    public Set<String> getChildTypes() {
        return Collections.singleton(CONTEXT);
    }

    @Override
    public Set<String> getChildrenNames(final String childType) {
        if (CONTEXT.equals(childType)) {

            ModClusterService service = ModClusterResource.service(modClusterName);
            if (service == null) {
                return Collections.emptySet();
            }
            ModCluster modCluster = service.getModCluster();
            ModClusterStatus status = modCluster.getController().getStatus();
            final Set<String> result = new LinkedHashSet<>();
            ModClusterStatus.LoadBalancer balancer = status.getLoadBalancer(this.balancerName);
            ModClusterStatus.Node node = balancer.getNode(this.name);
            for (ModClusterStatus.Context context :  node.getContexts()) {
                result.add(context.getName());
            }
            return result;
        }
        return null;
    }

    @Override
    public Set<ResourceEntry> getChildren(final String childType) {
        if (CONTEXT.equals(childType)) {
            final Set<String> names = getChildrenNames(childType);
            final Set<ResourceEntry> result = new LinkedHashSet<>(names.size());
            for (String name : names) {
                result.add(new PlaceholderResource.PlaceholderResourceEntry(PathElement.pathElement(CONTEXT, name)));
            }
            return result;
        }
        return Collections.emptySet();
    }

    @Override
    public void registerChild(final PathElement address, final Resource resource) {
        final String type = address.getKey();
        throw UndertowLogger.ROOT_LOGGER.cannotRegisterResourceOfType(type);
    }

    @Override
    public void registerChild(PathElement address, int index, Resource resource) {
        final String type = address.getKey();
        throw UndertowLogger.ROOT_LOGGER.cannotRegisterResourceOfType(type);
    }

    @Override
    public Resource removeChild(final PathElement address) {
        final String type = address.getKey();
        throw UndertowLogger.ROOT_LOGGER.cannotRemoveResourceOfType(type);
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
    public Set<String> getOrderedChildTypes() {
        return Collections.emptySet();
    }

    @Override
    public Resource clone() {
        return new ModClusterNodeResource(name, balancerName, modClusterName);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public PathElement getPathElement() {
        return PathElement.pathElement(Constants.NODE, name);
    }
}
