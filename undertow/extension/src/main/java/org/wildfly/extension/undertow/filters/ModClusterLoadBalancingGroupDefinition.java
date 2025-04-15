/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import io.undertow.server.handlers.proxy.mod_cluster.ModCluster;
import io.undertow.server.handlers.proxy.mod_cluster.ModClusterStatus;

import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.clustering.controller.OperationExecutor;
import org.jboss.as.clustering.controller.OperationFunction;
import org.jboss.as.clustering.controller.OperationHandler;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
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
public class ModClusterLoadBalancingGroupDefinition extends SimpleResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.LOAD_BALANCING_GROUP);
    static final ResourceDescriptionResolver RESOLVER = UndertowExtension.getResolver(Constants.FILTER, ModClusterDefinition.PATH_ELEMENT.getKey(), ModClusterBalancerDefinition.PATH_ELEMENT.getKey(), PATH_ELEMENT.getKey());

    enum LoadBalancingGroupOperation implements Operation<Map.Entry<ModClusterStatus.LoadBalancer, String>> {
        ENABLE_NODES(Constants.ENABLE_NODES, ModClusterStatus.Context::enable),
        DISABLE_NODES(Constants.DISABLE_NODES, ModClusterStatus.Context::disable),
        STOP_NODES(Constants.STOP_NODES, ModClusterStatus.Context::stop),
        ;
        private OperationDefinition definition;
        private final Consumer<ModClusterStatus.Context> operation;

        LoadBalancingGroupOperation(String name, Consumer<ModClusterStatus.Context> operation) {
            this.definition = SimpleOperationDefinitionBuilder.of(name, RESOLVER).setRuntimeOnly().build();
            this.operation = operation;
        }

        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, Map.Entry<ModClusterStatus.LoadBalancer, String> entry) {
            ModClusterStatus.LoadBalancer balancer = entry.getKey();
            String groupName = entry.getValue();
            for (ModClusterStatus.Node node : balancer.getNodes()) {
                if (groupName.equals(node.getDomain())) {
                    for (ModClusterStatus.Context context : node.getContexts()) {
                        this.operation.accept(context);
                    }
                }
            }
            return null;
        }

        @Override
        public OperationDefinition getDefinition() {
            return this.definition;
        }
    }

    private final FunctionExecutorRegistry<ModCluster> registry;

    ModClusterLoadBalancingGroupDefinition(FunctionExecutorRegistry<ModCluster> registry) {
        super(new Parameters(PATH_ELEMENT, RESOLVER).setRuntime());
        this.registry = registry;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        new OperationHandler<>(new LoadBalancingGroupOperationExecutor(this.registry), LoadBalancingGroupOperation.class).register(resourceRegistration);
    }

    static final Function<OperationContext, Function<ModCluster, Map.Entry<ModClusterStatus.LoadBalancer, String>>> LOAD_BALANCING_GROUP_FUNCTION_FACTORY = new Function<>() {
        @Override
        public Function<ModCluster, Map.Entry<ModClusterStatus.LoadBalancer, String>> apply(OperationContext context) {
            PathAddress groupAddress = context.getCurrentAddress();
            String groupName = groupAddress.getLastElement().getValue();
            PathAddress balancerAddress = groupAddress.getParent();
            String balancerName = balancerAddress.getLastElement().getValue();
            return new LoadBalancingGroupFunction(balancerName, groupName);
        }
    };

    static class LoadBalancingGroupFunction implements Function<ModCluster, Map.Entry<ModClusterStatus.LoadBalancer, String>> {
        private final String balancerName;
        private final String groupName;

        LoadBalancingGroupFunction(String balancerName, String groupName) {
            this.balancerName = balancerName;
            this.groupName = groupName;
        }

        @Override
        public Map.Entry<ModClusterStatus.LoadBalancer, String> apply(ModCluster service) {
            ModClusterStatus.LoadBalancer balancer = service.getController().getStatus().getLoadBalancer(this.balancerName);
            return (balancer != null) ? Map.entry(balancer, this.groupName) : null;
        }
    }

    static class LoadBalancingGroupOperationExecutor implements OperationExecutor<Map.Entry<ModClusterStatus.LoadBalancer, String>> {
        private final FunctionExecutorRegistry<ModCluster> registry;

        LoadBalancingGroupOperationExecutor(FunctionExecutorRegistry<ModCluster> registry) {
            this.registry = registry;
        }

        @Override
        public ModelNode execute(OperationContext context, ModelNode op, Operation<Map.Entry<ModClusterStatus.LoadBalancer, String>> operation) throws OperationFailedException {
            PathAddress serviceAddress = context.getCurrentAddress().getParent().getParent();
            FunctionExecutor<ModCluster> executor = this.registry.getExecutor(ServiceDependency.on(new ModClusterServiceNameProvider(serviceAddress).getServiceName()));
            Function<ModCluster, Map.Entry<ModClusterStatus.LoadBalancer, String>> mapper = LOAD_BALANCING_GROUP_FUNCTION_FACTORY.apply(context);
            return (executor != null) ? executor.execute(new OperationFunction<>(context, op, mapper, operation)) : null;
        }
    }
}
