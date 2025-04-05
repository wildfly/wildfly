/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.function.Function;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.service.capture.FunctionExecutorRegistry;
import org.wildfly.subsystem.resource.executor.Metric;
import org.wildfly.subsystem.resource.executor.MetricExecutor;
import org.wildfly.subsystem.resource.executor.MetricFunction;

/**
 * Generic executor for singleton metrics.
 * @author Paul Ferraro
 */
public class SingletonMetricExecutor implements MetricExecutor<Singleton> {

    private final Function<String, ServiceName> serviceNameFactory;
    private final FunctionExecutorRegistry<ServiceName, Singleton> executors;

    public SingletonMetricExecutor(Function<String, ServiceName> serviceNameFactory, FunctionExecutorRegistry<ServiceName, Singleton> executors) {
        this.serviceNameFactory = serviceNameFactory;
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, Metric<Singleton> metric) throws OperationFailedException {
        ServiceName name = this.serviceNameFactory.apply(context.getCurrentAddressValue());
        FunctionExecutor<Singleton> executor = this.executors.getExecutor(name);
        return (executor != null) ? executor.execute(new MetricFunction<>(Function.identity(), metric)) : null;
    }
}
