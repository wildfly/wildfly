/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.MetricExecutor;
import org.jboss.as.clustering.controller.MetricFunction;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.common.function.ExceptionFunction;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.service.capture.FunctionExecutorRegistry;

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
        FunctionExecutor<Singleton> executor = this.executors.getExecutor(name.append("singleton"));
        return ((executor != null) ? executor : new LegacySingletonFunctionExecutor(context, name)).execute(new MetricFunction<>(Function.identity(), metric));
    }

    @Deprecated
    private static class LegacySingletonFunctionExecutor implements FunctionExecutor<Singleton> {
        private final Singleton singleton;

        @SuppressWarnings("unchecked")
        LegacySingletonFunctionExecutor(OperationContext context, ServiceName name) {
            this.singleton = (Singleton) ((Supplier<org.jboss.msc.service.Service<?>>) context.getServiceRegistry(false).getRequiredService(name).getService()).get();
        }

        @Override
        public <R, E extends Exception> R execute(ExceptionFunction<Singleton, R, E> function) throws E {
            return function.apply(this.singleton);
        }
    }
}
