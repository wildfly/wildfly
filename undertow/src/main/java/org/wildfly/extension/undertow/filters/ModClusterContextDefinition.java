/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import java.util.EnumSet;
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
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
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
 * Runtime representation of a mod_cluster context
 *
 * @author Stuart Douglas
 */
class ModClusterContextDefinition extends SimpleResourceDefinition {

    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.CONTEXT);
    static final ResourceDescriptionResolver RESOLVER = UndertowExtension.getResolver(Constants.FILTER, ModClusterDefinition.PATH_ELEMENT.getKey(), ModClusterBalancerDefinition.PATH_ELEMENT.getKey(), ModClusterNodeDefinition.PATH_ELEMENT.getKey(), PATH_ELEMENT.getKey());

    enum ContextMetric implements Metric<ModClusterStatus.Context> {
        STATUS(Constants.STATUS, ModelType.STRING) {
            @Override
            public ModelNode execute(ModClusterStatus.Context context) {
                return new ModelNode(context.isEnabled() ? "enabled" : context.isStopped() ? "stopped" : "disabled");
            }
        },
        REQUESTS(Constants.REQUESTS, ModelType.INT) {
            @Override
            public ModelNode execute(ModClusterStatus.Context context) {
                return new ModelNode(context.getRequests());
            }
        },
        ;
        private final AttributeDefinition definition;

        ContextMetric(String name, ModelType type) {
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

    enum ContextOperation implements Operation<ModClusterStatus.Context> {
        ENABLE(Constants.ENABLE, ModClusterStatus.Context::enable),
        DISABLE(Constants.DISABLE, ModClusterStatus.Context::disable),
        STOP(Constants.STOP, ModClusterStatus.Context::stop),
        ;
        private OperationDefinition definition;
        private final Consumer<ModClusterStatus.Context> operation;

        ContextOperation(String name, Consumer<ModClusterStatus.Context> operation) {
            this.definition = SimpleOperationDefinitionBuilder.of(name, RESOLVER).setRuntimeOnly().build();
            this.operation = operation;
        }

        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterStatus.Context context) {
            this.operation.accept(context);
            return null;
        }

        @Override
        public OperationDefinition getDefinition() {
            return this.definition;
        }
    }

    private final FunctionExecutorRegistry<ModCluster> registry;

    ModClusterContextDefinition(FunctionExecutorRegistry<ModCluster> registry) {
        super(new Parameters(PATH_ELEMENT, RESOLVER).setRuntime());
        this.registry = registry;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler handler = new MetricHandler<>(new ContextMetricExecutor(new FunctionExecutorFactory(this.registry)), ContextMetric.class);
        // TODO Should some subset of these be registered as metrics?
        for (ContextMetric metric : EnumSet.allOf(ContextMetric.class)) {
            resourceRegistration.registerReadOnlyAttribute(metric.getDefinition(), handler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        new OperationHandler<>(new ContextOperationExecutor(new FunctionExecutorFactory(this.registry)), ContextOperation.class).register(resourceRegistration);
    }

    static class FunctionExecutorFactory implements Function<OperationContext, FunctionExecutor<ModCluster>> {
        private final FunctionExecutorRegistry<ModCluster> registry;

        FunctionExecutorFactory(FunctionExecutorRegistry<ModCluster> registry) {
            this.registry = registry;
        }

        @Override
        public FunctionExecutor<ModCluster> apply(OperationContext context) {
            PathAddress serviceAddress = context.getCurrentAddress().getParent().getParent().getParent();
            return this.registry.getExecutor(ServiceDependency.on(new ModClusterServiceNameProvider(serviceAddress).getServiceName()));
        }
    }

    static final Function<OperationContext, Function<ModCluster, ModClusterStatus.Context>> NODE_FUNCTION_FACTORY = new Function<>() {
        @Override
        public Function<ModCluster, ModClusterStatus.Context> apply(OperationContext context) {
            PathAddress contextAddress = context.getCurrentAddress();
            String contextName = contextAddress.getLastElement().getValue();
            PathAddress nodeAddress = contextAddress.getParent();
            String nodeName = nodeAddress.getLastElement().getValue();
            PathAddress balancerAddress = nodeAddress.getParent();
            String balancerName = balancerAddress.getLastElement().getValue();
            return new ContextFunction(balancerName, nodeName, contextName);
        }
    };

    static class ContextFunction implements Function<ModCluster, ModClusterStatus.Context> {
        private final String balancerName;
        private final String nodeName;
        private final String contextName;

        ContextFunction(String balancerName, String nodeName, String contextName) {
            this.balancerName = balancerName;
            this.nodeName = nodeName;
            this.contextName = contextName;
        }

        @Override
        public ModClusterStatus.Context apply(ModCluster service) {
            ModClusterStatus.LoadBalancer balancer = service.getController().getStatus().getLoadBalancer(this.balancerName);
            ModClusterStatus.Node node = (balancer != null) ? balancer.getNode(this.nodeName) : null;
            return (node != null) ? node.getContext(this.contextName) : null;
        }
    }

    static class ContextMetricExecutor implements MetricExecutor<ModClusterStatus.Context> {
        private final Function<OperationContext, FunctionExecutor<ModCluster>> factory;

        ContextMetricExecutor(Function<OperationContext, FunctionExecutor<ModCluster>> factory) {
            this.factory = factory;
        }

        @Override
        public ModelNode execute(OperationContext context, Metric<ModClusterStatus.Context> metric) throws OperationFailedException {
            FunctionExecutor<ModCluster> executor = this.factory.apply(context);
            Function<ModCluster, ModClusterStatus.Context> mapper = NODE_FUNCTION_FACTORY.apply(context);
            return (executor != null) ? executor.execute(new MetricFunction<>(mapper, metric)) : null;
        }
    }

    static class ContextOperationExecutor implements OperationExecutor<ModClusterStatus.Context> {
        private final Function<OperationContext, FunctionExecutor<ModCluster>> factory;

        ContextOperationExecutor(Function<OperationContext, FunctionExecutor<ModCluster>> factory) {
            this.factory = factory;
        }

        @Override
        public ModelNode execute(OperationContext context, ModelNode op, Operation<ModClusterStatus.Context> operation) throws OperationFailedException {
            FunctionExecutor<ModCluster> executor = this.factory.apply(context);
            Function<ModCluster, ModClusterStatus.Context> mapper = NODE_FUNCTION_FACTORY.apply(context);
            return (executor != null) ? executor.execute(new OperationFunction<>(context, op, mapper, operation)) : null;
        }
    }
}
