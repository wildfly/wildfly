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
import io.undertow.util.CopyOnWriteMap;

import org.jboss.as.clustering.controller.ChildResourceProvider;
import org.jboss.as.clustering.controller.ComplexResource;
import org.jboss.as.clustering.controller.SimpleChildResourceProvider;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Runtime resource implementation for {@link ModCluster}.
 * @author Paul Ferraro
 */
public class ModClusterResource extends ComplexResource implements Consumer<ModCluster> {
    private final Map<String, ChildResourceProvider> providers;

    public ModClusterResource(Resource resource) {
        this(resource, new CopyOnWriteMap<>());
    }

    private ModClusterResource(Resource resource, Map<String, ChildResourceProvider> providers) {
        super(resource, providers, ModClusterResource::new);
        this.providers = providers;
    }

    @Override
    public void accept(ModCluster service) {
        this.providers.put(ModClusterBalancerDefinition.PATH_ELEMENT.getKey(), new ModClusterBalancerResourceProvider(service));
    }

    static class ModClusterBalancerResourceProvider implements ChildResourceProvider {
        private final ModCluster service;

        ModClusterBalancerResourceProvider(ModCluster service) {
            this.service = service;
        }

        @Override
        public Resource getChild(String name) {
            ModClusterStatus status = this.service.getController().getStatus();
            ModClusterStatus.LoadBalancer balancer = status.getLoadBalancer(name);
            return (balancer != null) ? new ComplexResource(PlaceholderResource.INSTANCE, Map.of(ModClusterNodeDefinition.PATH_ELEMENT.getKey(), new ModClusterNodeResourceProvider(balancer), ModClusterLoadBalancingGroupDefinition.PATH_ELEMENT.getKey(), new SimpleChildResourceProvider(balancer.getNodes().stream().map(ModClusterStatus.Node::getDomain).filter(Predicate.not(Objects::isNull)).distinct().collect(Collectors.toSet())))) : null;
        }

        @Override
        public Set<String> getChildren() {
            ModClusterStatus status = this.service.getController().getStatus();
            return status.getLoadBalancers().stream().map(ModClusterStatus.LoadBalancer::getName).collect(Collectors.toSet());
        }
    }

    static class ModClusterNodeResourceProvider implements ChildResourceProvider {
        private final ModClusterStatus.LoadBalancer balancer;

        ModClusterNodeResourceProvider(ModClusterStatus.LoadBalancer balancer) {
            this.balancer = balancer;
        }

        @Override
        public Resource getChild(String name) {
            ModClusterStatus.Node node = this.balancer.getNode(name);
            return (node != null) ? new ComplexResource(PlaceholderResource.INSTANCE, Map.of(ModClusterContextDefinition.PATH_ELEMENT.getKey(), new SimpleChildResourceProvider(node.getContexts().stream().map(ModClusterStatus.Context::getName).collect(Collectors.toSet())))) : null;
        }

        @Override
        public Set<String> getChildren() {
            return this.balancer.getNodes().stream().map(ModClusterStatus.Node::getName).collect(Collectors.toSet());
        }
    }
}
