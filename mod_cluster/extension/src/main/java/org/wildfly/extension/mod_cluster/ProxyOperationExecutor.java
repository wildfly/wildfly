/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.mod_cluster;

import java.util.function.Function;

import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.clustering.controller.OperationExecutor;
import org.jboss.as.clustering.controller.OperationFunction;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.ModClusterServiceMBean;
import org.jboss.msc.service.ServiceName;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Radoslav Husar
 */
public class ProxyOperationExecutor implements OperationExecutor<ModClusterServiceMBean> {

    public static final SimpleAttributeDefinition HOST = SimpleAttributeDefinitionBuilder.create("host", ModelType.STRING, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .build();

    public static final SimpleAttributeDefinition PORT = SimpleAttributeDefinitionBuilder.create("port", ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new IntRangeValidator(1, Short.MAX_VALUE - Short.MIN_VALUE, false, false))
            .setStorageRuntime()
            .build();

    static final SimpleAttributeDefinition VIRTUAL_HOST = SimpleAttributeDefinitionBuilder.create("virtualhost", ModelType.STRING, false)
            .addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .build();

    static final SimpleAttributeDefinition WAIT_TIME = SimpleAttributeDefinitionBuilder.create("waittime", ModelType.INT, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .setDefaultValue(new ModelNode(10))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .build();

    static final SimpleAttributeDefinition CONTEXT = SimpleAttributeDefinitionBuilder.create("context", ModelType.STRING, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .build();

    private final FunctionExecutorRegistry<ModClusterServiceMBean> executors;

    public ProxyOperationExecutor(FunctionExecutorRegistry<ModClusterServiceMBean> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, ModelNode operation, Operation<ModClusterServiceMBean> executable) throws OperationFailedException {
        ServiceName serviceName = ProxyConfigurationResourceDefinition.Capability.SERVICE.getDefinition().getCapabilityServiceName(context.getCurrentAddress());
        FunctionExecutor<ModClusterServiceMBean> executor = this.executors.getExecutor(ServiceDependency.on(serviceName));
        return (executor != null) ? executor.execute(new OperationFunction<>(context, operation, Function.identity(), executable)) : null;
    }
}
