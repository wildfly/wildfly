/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.singleton;

import java.util.function.Function;

import org.jboss.as.clustering.controller.FunctionExecutor;
import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.MetricExecutor;
import org.jboss.as.clustering.controller.MetricFunction;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Generic executor for singleton metrics.
 * @author Paul Ferraro
 */
public class SingletonMetricExecutor implements MetricExecutor<Singleton> {

    private final Function<String, ServiceName> serviceNameFactory;
    private final FunctionExecutorRegistry<Singleton> executors;

    public SingletonMetricExecutor(Function<String, ServiceName> serviceNameFactory, FunctionExecutorRegistry<Singleton> executors) {
        this.serviceNameFactory = serviceNameFactory;
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, Metric<Singleton> metric) throws OperationFailedException {
        ServiceName name = this.serviceNameFactory.apply(context.getCurrentAddressValue());
        FunctionExecutor<Singleton> executor = this.executors.get(name.append("singleton"));
        return ((executor != null) ? executor : new LegacySingletonFunctionExecutor(context, name)).execute(new MetricFunction<>(Function.identity(), metric));
    }

    private static class LegacySingletonFunctionExecutor implements FunctionExecutor<Singleton> {
        private final Singleton singleton;

        @SuppressWarnings("deprecation")
        LegacySingletonFunctionExecutor(OperationContext context, ServiceName name) {
            this.singleton = (Singleton) ((org.wildfly.clustering.service.AsynchronousServiceBuilder<?>) context.getServiceRegistry(false).getRequiredService(name).getService()).getService();
        }

        @Override
        public <R, E extends Exception> R execute(ExceptionFunction<Singleton, R, E> function) throws E {
            return function.apply(this.singleton);
        }
    }
}
