/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import io.undertow.server.handlers.proxy.mod_cluster.ModCluster;
import io.undertow.server.handlers.proxy.mod_cluster.ModClusterStatus;

import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.MetricExecutor;
import org.jboss.as.clustering.controller.MetricFunction;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.clustering.controller.OperationExecutor;
import org.jboss.as.clustering.controller.OperationFunction;
import org.jboss.as.clustering.controller.OperationHandler;
import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Runtime representation of a mod_cluster node
 *
 * @author Stuart Douglas
 */
public class ModClusterNodeDefinition extends SimpleResourceDefinition {

    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.NODE);
    static final ResourceDescriptionResolver RESOLVER = UndertowExtension.getResolver(Constants.FILTER, ModClusterDefinition.PATH_ELEMENT.getKey(), ModClusterBalancerDefinition.PATH_ELEMENT.getKey(), PATH_ELEMENT.getKey());
    static final BiFunction<String, ModelType, PrimitiveListAttributeDefinition.Builder> PRIMITIVE_LIST_BUILDER_FACTORY = PrimitiveListAttributeDefinition.Builder::new;

    enum NodeMetric implements Metric<ModClusterStatus.Node> {
        LOAD(Constants.LOAD, ModelType.INT) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return new ModelNode(node.getLoad());
            }
        },
        STATUS(Constants.STATUS, ModelType.STRING) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return new ModelNode(node.getStatus().name());
            }
        },
        LOAD_BALANCING_GROUP(Constants.LOAD_BALANCING_GROUP, ModelType.STRING) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return Optional.ofNullable(node.getDomain()).map(ModelNode::new).orElse(null);
            }
        },
        CACHE_CONNECTIONS(Constants.CACHE_CONNECTIONS, ModelType.INT) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return new ModelNode(node.getCacheConnections());
            }
        },
        MAX_CONNECTIONS(Constants.MAX_CONNECTIONS, ModelType.INT) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return new ModelNode(node.getMaxConnections());
            }
        },
        OPEN_CONNECTIONS(Constants.OPEN_CONNECTIONS, ModelType.INT) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return new ModelNode(node.getOpenConnections());
            }
        },
        PING(Constants.PING, ModelType.INT) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return new ModelNode(node.getPing());
            }
        },
        READ(Constants.READ, ModelType.LONG) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return new ModelNode(node.getRead());
            }

            @Override
            <D extends AttributeDefinition, B extends AbstractAttributeDefinitionBuilder<B, D>> B configure(B builder) {
                return builder.setMeasurementUnit(MeasurementUnit.BYTES);
            }
        },
        REQUEST_QUEUE_SIZE(Constants.REQUEST_QUEUE_SIZE, ModelType.INT) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return new ModelNode(node.getRequestQueueSize());
            }
        },
        TIMEOUT(Constants.TIMEOUT, ModelType.INT) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return new ModelNode(node.getTimeout());
            }

            @Override
            <D extends AttributeDefinition, B extends AbstractAttributeDefinitionBuilder<B, D>> B configure(B builder) {
                return builder.setMeasurementUnit(MeasurementUnit.SECONDS);
            }
        },
        WRITTEN(Constants.WRITTEN, ModelType.LONG) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return new ModelNode(node.getTransferred());
            }

            @Override
            <D extends AttributeDefinition, B extends AbstractAttributeDefinitionBuilder<B, D>> B configure(B builder) {
                return builder.setMeasurementUnit(MeasurementUnit.BYTES);
            }
        },
        TTL(Constants.TTL, ModelType.LONG) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return new ModelNode(node.getTtl());
            }
        },
        FLUSH_PACKETS(Constants.FLUSH_PACKETS, ModelType.BOOLEAN) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return node.isFlushPackets() ? ModelNode.TRUE : ModelNode.FALSE;
            }
        },
        QUEUE_NEW_REQUESTS(Constants.QUEUE_NEW_REQUESTS, ModelType.BOOLEAN) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return node.isQueueNewRequests() ? ModelNode.TRUE : ModelNode.FALSE;
            }
        },
        URI(Constants.URI, ModelType.STRING) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return new ModelNode(node.getUri().toString());
            }
        },
        ALIASES(Constants.ALIASES, ModelType.STRING, PRIMITIVE_LIST_BUILDER_FACTORY) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                ModelNode result = new ModelNode();
                for (String alias : node.getAliases()) {
                    result.add(alias);
                }
                return result;
            }
        },
        ELECTED(Constants.ELECTED, ModelType.INT) {
            @Override
            public ModelNode execute(ModClusterStatus.Node node) {
                return new ModelNode(node.getElected());
            }
        },
        ;

        private final AttributeDefinition definition;

        NodeMetric(String name, ModelType type) {
            this(name, type, SimpleAttributeDefinitionBuilder::new);
        }

        <D extends AttributeDefinition, B extends AbstractAttributeDefinitionBuilder<B, D>> NodeMetric(String name, ModelType type, BiFunction<String, ModelType, B> builderFactory) {
            this.definition = this.configure(builderFactory.apply(name, type))
                    .setRequired(false)
                    .setStorageRuntime()
                    .build();
        }

        <D extends AttributeDefinition, B extends AbstractAttributeDefinitionBuilder<B, D>> B configure(B builder) {
            return builder;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    enum NodeOperation implements Operation<ModClusterStatus.Node> {
        ENABLE(Constants.ENABLE, ModClusterStatus.Context::enable),
        DISABLE(Constants.DISABLE, ModClusterStatus.Context::disable),
        STOP(Constants.STOP, ModClusterStatus.Context::stop),
        ;
        private OperationDefinition definition;
        private final Consumer<ModClusterStatus.Context> operation;

        NodeOperation(String name, Consumer<ModClusterStatus.Context> operation) {
            this.definition = SimpleOperationDefinitionBuilder.of(name, RESOLVER).setRuntimeOnly().build();
            this.operation = operation;
        }

        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterStatus.Node node) {
            node.getContexts().forEach(this.operation);
            return null;
        }

        @Override
        public OperationDefinition getDefinition() {
            return this.definition;
        }
    }

    private final FunctionExecutorRegistry<ModCluster> registry;

    ModClusterNodeDefinition(FunctionExecutorRegistry<ModCluster> registry) {
        super(new Parameters(PATH_ELEMENT, RESOLVER).setRuntime());
        this.registry = registry;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new ModClusterContextDefinition(this.registry));
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        new OperationHandler<>(new NodeOperationExecutor(new FunctionExecutorFactory(this.registry)), NodeOperation.class).register(resourceRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler handler = new MetricHandler<>(new NodeMetricExecutor(new FunctionExecutorFactory(this.registry)), NodeMetric.class);
        // TODO Consider registering some subset of these as proper metrics
        for (NodeMetric metric : EnumSet.allOf(NodeMetric.class)) {
            resourceRegistration.registerReadOnlyAttribute(metric.getDefinition(), handler);
        }
    }

    static class FunctionExecutorFactory implements Function<OperationContext, FunctionExecutor<ModCluster>> {
        private final FunctionExecutorRegistry<ModCluster> registry;

        FunctionExecutorFactory(FunctionExecutorRegistry<ModCluster> registry) {
            this.registry = registry;
        }

        @Override
        public FunctionExecutor<ModCluster> apply(OperationContext context) {
            PathAddress serviceAddress = context.getCurrentAddress().getParent().getParent();
            return this.registry.getExecutor(ServiceDependency.on(new ModClusterServiceNameProvider(serviceAddress).getServiceName()));
        }
    }

    static final Function<OperationContext, Function<ModCluster, ModClusterStatus.Node>> NODE_FUNCTION_FACTORY = new Function<>() {
        @Override
        public Function<ModCluster, ModClusterStatus.Node> apply(OperationContext context) {
            PathAddress nodeAddress = context.getCurrentAddress();
            String nodeName = nodeAddress.getLastElement().getValue();
            PathAddress balancerAddress = nodeAddress.getParent();
            String balancerName = balancerAddress.getLastElement().getValue();
            return new NodeFunction(balancerName, nodeName);
        }
    };

    static class NodeMetricExecutor implements MetricExecutor<ModClusterStatus.Node> {
        private final Function<OperationContext, FunctionExecutor<ModCluster>> factory;

        NodeMetricExecutor(Function<OperationContext, FunctionExecutor<ModCluster>> factory) {
            this.factory = factory;
        }

        @Override
        public ModelNode execute(OperationContext context, Metric<ModClusterStatus.Node> metric) throws OperationFailedException {
            FunctionExecutor<ModCluster> executor = this.factory.apply(context);
            Function<ModCluster, ModClusterStatus.Node> mapper = NODE_FUNCTION_FACTORY.apply(context);
            return (executor != null) ? executor.execute(new MetricFunction<>(mapper, metric)) : null;
        }
    }

    static class NodeOperationExecutor implements OperationExecutor<ModClusterStatus.Node> {
        private final Function<OperationContext, FunctionExecutor<ModCluster>> factory;

        NodeOperationExecutor(Function<OperationContext, FunctionExecutor<ModCluster>> factory) {
            this.factory = factory;
        }

        @Override
        public ModelNode execute(OperationContext context, ModelNode op, Operation<ModClusterStatus.Node> operation) throws OperationFailedException {
            FunctionExecutor<ModCluster> executor = this.factory.apply(context);
            Function<ModCluster, ModClusterStatus.Node> mapper = NODE_FUNCTION_FACTORY.apply(context);
            return (executor != null) ? executor.execute(new OperationFunction<>(context, op, mapper, operation)) : null;
        }
    }

    static class NodeFunction implements Function<ModCluster, ModClusterStatus.Node> {
        private final String balancerName;
        private final String nodeName;

        NodeFunction(String balancerName, String nodeName) {
            this.balancerName = balancerName;
            this.nodeName = nodeName;
        }

        @Override
        public ModClusterStatus.Node apply(ModCluster service) {
            ModClusterStatus.LoadBalancer balancer = service.getController().getStatus().getLoadBalancer(this.balancerName);
            return (balancer != null) ? balancer.getNode(this.nodeName) : null;
        }
    }
}
