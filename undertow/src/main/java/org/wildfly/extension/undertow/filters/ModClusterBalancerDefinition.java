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

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Function;

import io.undertow.server.handlers.proxy.mod_cluster.ModCluster;
import io.undertow.server.handlers.proxy.mod_cluster.ModClusterStatus;
import io.undertow.server.handlers.proxy.mod_cluster.ModClusterStatus.LoadBalancer;

import org.jboss.as.clustering.controller.FunctionExecutor;
import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.MetricExecutor;
import org.jboss.as.clustering.controller.MetricFunction;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;

/**
 * Runtime representation of a mod_cluster balancer
 *
 * @author Stuart Douglas
 */
class ModClusterBalancerDefinition extends SimpleResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.BALANCER);

    enum LoadBalancerMetric implements Metric<ModClusterStatus.LoadBalancer> {
        STICKY_SESSION_COOKIE(Constants.STICKY_SESSION_COOKIE, ModelType.STRING) {
            @Override
            public ModelNode execute(LoadBalancer balancer) {
                return Optional.ofNullable(balancer.getStickySessionCookie()).map(ModelNode::new).orElse(null);
            }
        },
        STICKY_SESSION_PATH(Constants.STICKY_SESSION_PATH, ModelType.STRING) {
            @Override
            public ModelNode execute(LoadBalancer balancer) {
                return Optional.ofNullable(balancer.getStickySessionPath()).map(ModelNode::new).orElse(null);
            }
        },
        MAX_ATTEMPTS(Constants.MAX_ATTEMPTS, ModelType.INT) {
            @Override
            public ModelNode execute(LoadBalancer balancer) {
                return new ModelNode(balancer.getMaxRetries());
            }
        },
        WAIT_WORKER(Constants.WAIT_WORKER, ModelType.INT) {
            @Override
            public ModelNode execute(LoadBalancer balancer) {
                return new ModelNode(balancer.getWaitWorker());
            }
        },
        STICKY_SESSION(Constants.STICKY_SESSION, ModelType.BOOLEAN) {
            @Override
            public ModelNode execute(LoadBalancer balancer) {
                return balancer.isStickySession() ? ModelNode.TRUE : ModelNode.FALSE;
            }
        },
        STICKY_SESSION_FORCE(Constants.STICKY_SESSION_FORCE, ModelType.BOOLEAN) {
            @Override
            public ModelNode execute(LoadBalancer balancer) {
                return balancer.isStickySessionForce() ? ModelNode.TRUE : ModelNode.FALSE;
            }
        },
        STICKY_SESSION_REMOVE(Constants.STICKY_SESSION_REMOVE, ModelType.BOOLEAN) {
            @Override
            public ModelNode execute(LoadBalancer balancer) {
                return balancer.isStickySessionRemove() ? ModelNode.TRUE : ModelNode.FALSE;
            }
        },
        ;
        private final AttributeDefinition definition;

        LoadBalancerMetric(String name, ModelType type) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(false)
                    .setStorageRuntime()
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private final FunctionExecutorRegistry<ModCluster> registry;

    ModClusterBalancerDefinition(FunctionExecutorRegistry<ModCluster> registry) {
        super(new Parameters(PATH_ELEMENT, UndertowExtension.getResolver(Constants.FILTER, ModClusterDefinition.PATH_ELEMENT.getKey(), PATH_ELEMENT.getKey())).setRuntime());
        this.registry = registry;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new ModClusterNodeDefinition(this.registry));
        resourceRegistration.registerSubModel(new ModClusterLoadBalancingGroupDefinition(this.registry));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler handler = new MetricHandler<>(new LoadBalancerMetricExecutor(this.registry), LoadBalancerMetric.class);
        // TODO Should some subset of these be registered as metrics?
        for (LoadBalancerMetric metric : EnumSet.allOf(LoadBalancerMetric.class)) {
            resourceRegistration.registerReadOnlyAttribute(metric.getDefinition(), handler);
        }
    }

    static class LoadBalancerMetricExecutor implements MetricExecutor<ModClusterStatus.LoadBalancer> {
        private final FunctionExecutorRegistry<ModCluster> registry;

        LoadBalancerMetricExecutor(FunctionExecutorRegistry<ModCluster> registry) {
            this.registry = registry;
        }

        @Override
        public ModelNode execute(OperationContext context, Metric<ModClusterStatus.LoadBalancer> metric) throws OperationFailedException {
            PathAddress balancerAddress = context.getCurrentAddress();
            String balancerName = balancerAddress.getLastElement().getValue();
            PathAddress serviceAddress = balancerAddress.getParent();
            FunctionExecutor<ModCluster> executor = this.registry.get(new ModClusterServiceNameProvider(serviceAddress).getServiceName());
            return (executor != null) ? executor.execute(new MetricFunction<>(new LoadBalancerFunction(balancerName), metric)) : null;
        }
    }

    static class LoadBalancerFunction implements Function<ModCluster, ModClusterStatus.LoadBalancer> {
        private final String balancerName;

        LoadBalancerFunction(String balancerName) {
            this.balancerName = balancerName;
        }

        @Override
        public LoadBalancer apply(ModCluster service) {
            return service.getController().getStatus().getLoadBalancer(this.balancerName);
        }
    }
}