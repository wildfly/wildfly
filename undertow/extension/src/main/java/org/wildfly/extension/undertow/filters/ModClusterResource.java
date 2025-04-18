/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
