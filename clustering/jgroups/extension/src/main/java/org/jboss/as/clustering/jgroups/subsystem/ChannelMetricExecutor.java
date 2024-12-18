/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Function;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.JGroupsServiceDescriptor;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.subsystem.resource.executor.Metric;
import org.wildfly.subsystem.resource.executor.MetricExecutor;
import org.wildfly.subsystem.resource.executor.MetricFunction;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Handler for reading run-time only attributes from an underlying channel service.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 * @author Paul Ferraro
 */
public class ChannelMetricExecutor implements MetricExecutor<JChannel> {

    private final FunctionExecutorRegistry<JChannel> executors;

    public ChannelMetricExecutor(FunctionExecutorRegistry<JChannel> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, Metric<JChannel> metric) throws OperationFailedException {
        FunctionExecutor<JChannel> executor = this.executors.getExecutor(ServiceDependency.on(JGroupsServiceDescriptor.CHANNEL, context.getCurrentAddressValue()));
        return (executor != null) ? executor.execute(new MetricFunction<>(Function.identity(), metric)) : null;
    }
}
