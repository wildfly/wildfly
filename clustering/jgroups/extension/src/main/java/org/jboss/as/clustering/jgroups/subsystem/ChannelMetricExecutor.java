/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Function;

import org.jboss.as.clustering.controller.FunctionExecutor;
import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.MetricExecutor;
import org.jboss.as.clustering.controller.MetricFunction;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;

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
        ServiceName name = JGroupsRequirement.CHANNEL.getServiceName(context, UnaryCapabilityNameResolver.DEFAULT);
        FunctionExecutor<JChannel> executor = this.executors.get(name);
        return (executor != null) ? executor.execute(new MetricFunction<>(Function.identity(), metric)) : null;
    }
}
