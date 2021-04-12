/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Operation step handler that expose a capability's metrics, through its service.
 *
 * @author emmartins
 */
public class ManagedExecutorServiceMetricsHandler<T> extends AbstractRuntimeOnlyHandler {

    private final Map<String, Metric<T>> metrics;
    private final RuntimeCapability capability;

    public static <T> Builder<T> builder(RuntimeCapability capability) {
        return new Builder<>(capability);
    }

    private ManagedExecutorServiceMetricsHandler(final Map<String, Metric<T>> metrics, final RuntimeCapability capability) {
        this.metrics = metrics;
        this.capability = capability;
    }

    /**
     * Registers metrics attr definitions.
     * @param registration
     */
    public void registerAttributes(final ManagementResourceRegistration registration) {
        for (Metric metric : metrics.values()) {
            registration.registerMetric(metric.attributeDefinition, this);
        }
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
        if (context.getRunningMode() == RunningMode.NORMAL) {
            ServiceName serviceName = capability.getCapabilityServiceName(context.getCurrentAddress());
            ServiceController<?> controller = context.getServiceRegistry(false).getService(serviceName);
            if (controller == null) {
                throw EeLogger.ROOT_LOGGER.executorServiceNotFound(serviceName);
            }
            final T service = (T) controller.getService();
            final Metric<T> metric = metrics.get(attributeName);
            if (metric == null) {
                throw EeLogger.ROOT_LOGGER.unsupportedExecutorServiceMetric(attributeName);
            }
            metric.resultSetter.setResult(context, service);
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    public static class Builder<T> {
        private final RuntimeCapability capability;
        private final Map<String, Metric<T>> metrics = new HashMap<>();

        public Builder(RuntimeCapability capability) {
            this.capability = capability;
        }

        /**
         *
         * @param attributeDefinition
         * @param resultSetter
         * @return
         */
        public Builder<T> addMetric(AttributeDefinition attributeDefinition, MetricResultSetter<T> resultSetter) {
            final String name = attributeDefinition.getName();
            metrics.put(name, new Metric<>(attributeDefinition, resultSetter));
            return this;
        }

        /**
         *
         * @return
         */
        public ManagedExecutorServiceMetricsHandler<T> build() {
            return new ManagedExecutorServiceMetricsHandler<>(Collections.unmodifiableMap(metrics), capability);
        }
    }

    private static class Metric<T> {
        final AttributeDefinition attributeDefinition;
        final MetricResultSetter<T> resultSetter;

        Metric(AttributeDefinition attributeDefinition, MetricResultSetter<T> resultSetter) {
            this.attributeDefinition = attributeDefinition;
            this.resultSetter = resultSetter;
        }
    }

    public interface MetricResultSetter<T> {
        void setResult(OperationContext context, T service) throws OperationFailedException;
    }
}
