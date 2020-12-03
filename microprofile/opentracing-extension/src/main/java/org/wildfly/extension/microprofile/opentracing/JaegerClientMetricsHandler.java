/*
 * Copyright 2020 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.microprofile.opentracing;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.metrics.Metrics;
import io.opentracing.Tracer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.jboss.as.controller.registry.AttributeAccess.Flag.COUNTER_METRIC;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.GAUGE_METRIC;

public class JaegerClientMetricsHandler extends AbstractRuntimeOnlyHandler {

    public enum JaegerClientMetric {

        TRACES_STARTED_SAMPLED(SimpleAttributeDefinitionBuilder.create("traces-started-sampled", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        TRACES_STARTED_NOT_SAMPLED(SimpleAttributeDefinitionBuilder.create("traces-started-not-sampled", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        TRACES_JOINED_SAMPLED(SimpleAttributeDefinitionBuilder.create("traces-joined-sampled", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        TRACES_JOINED_NOT_SAMPLED(SimpleAttributeDefinitionBuilder.create("traces-joined-not-sampled", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        SPANS_STARTED_SAMPLED(SimpleAttributeDefinitionBuilder.create("spans-started-sampled", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        SPANS_STARTED_NOT_SAMPLED(SimpleAttributeDefinitionBuilder.create("spans-started-not-sampled", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        SPANS_FINISHED(SimpleAttributeDefinitionBuilder.create("spans-finished", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        SPAN_CONTEXT_DECODING_ERRORS(SimpleAttributeDefinitionBuilder.create("span-context-decoding-errors", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        REPORTER_SPANS_REPORTED_SUCCESS(SimpleAttributeDefinitionBuilder.create("reporter-spans-reported-success", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        REPORTER_SPANS_REPORTED_FAILED(SimpleAttributeDefinitionBuilder.create("reporter-spans-reported-failed", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        REPORTER_SPANS_DROPPED(SimpleAttributeDefinitionBuilder.create("reporter-spans-dropped", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        REPORTER_QUEUE_LENGTH(SimpleAttributeDefinitionBuilder.create("reporter-queue-length", ModelType.LONG, true).setFlags(GAUGE_METRIC).build()),
        SAMPLER_QUERIES_SUCCESS(SimpleAttributeDefinitionBuilder.create("sampler-queries-success", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        SAMPLER_QUERIES_FAILED(SimpleAttributeDefinitionBuilder.create("sampler-queries-failed", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        SAMPLER_UPDATES_SUCCESS(SimpleAttributeDefinitionBuilder.create("sampler-updates-success", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        SAMPLER_UPDATES_FAILED(SimpleAttributeDefinitionBuilder.create("sampler-updates-failed", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        BAGGAGE_UPDATES_SUCCESS(SimpleAttributeDefinitionBuilder.create("baggage-updates-success", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        BAGGAGE_UPDATES_FAILED(SimpleAttributeDefinitionBuilder.create("baggage-updates-failed", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        BAGGAGE_TRUNCATIONS(SimpleAttributeDefinitionBuilder.create("baggage-truncations", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        BAGGAGE_RESTRICTIONS_UPDATES_SUCCESS(SimpleAttributeDefinitionBuilder.create("baggage-restrictions-updates-success", ModelType.LONG, true).setFlags(COUNTER_METRIC).build()),
        BAGGAGE_RESTRICTIONS_UPDATES_FAILED(SimpleAttributeDefinitionBuilder.create("baggage-restrictions-updates-failed", ModelType.LONG, true).setFlags(COUNTER_METRIC).build());

        private static final Map<String, JaegerClientMetric> MAP = new HashMap<>();

        static {
            for (JaegerClientMetric stat : EnumSet.allOf(JaegerClientMetric.class)) {
                MAP.put(stat.toString(), stat);
            }
        }

        final AttributeDefinition definition;

        JaegerClientMetric(final AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public final String toString() {
            return definition.getName();
        }

        public static JaegerClientMetric getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

    public static final JaegerClientMetricsHandler INSTANCE = new JaegerClientMetricsHandler();

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) {
        String metricName = operation.require(ModelDescriptionConstants.NAME).asString();

        JaegerClientMetric metric = JaegerClientMetric.getStat(metricName);
        if (metric == null) {
            context.getFailureDescription().set(TracingExtensionLogger.ROOT_LOGGER.unknownMetric(metricName));
            return;
        }

        ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        ServiceName serviceName = resolveDeploymentUnitServiceName(context, operation);
        ServiceController<?> serviceController = serviceRegistry.getRequiredService(serviceName);
        DeploymentUnit deploymentUnit = (DeploymentUnit) serviceController.getValue();

        Tracer tracer = deploymentUnit.getAttachment(TracingDeploymentProcessor.ATTACHMENT_KEY);
        if (tracer == null || !(tracer instanceof JaegerTracer)) {
            TracingExtensionLogger.ROOT_LOGGER.debug(TracingExtensionLogger.ROOT_LOGGER.notManagedJaegerTracer());
            context.getResult().set(new ModelNode(0));
            return;
        }

        Metrics metrics = ((JaegerTracer) tracer).getMetrics();
        if (metrics == null || !(metrics.traceStartedSampled instanceof WildflyJaegerMetricsFactory.Metric)) {
            TracingExtensionLogger.ROOT_LOGGER.debug(TracingExtensionLogger.ROOT_LOGGER.notManagedJaegerTracer());
            context.getResult().set(new ModelNode(0));
            return;
        }

        ModelNode result = new ModelNode();

        switch (metric) {
            case TRACES_STARTED_SAMPLED:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.traceStartedSampled).getValue());
                break;
            case TRACES_STARTED_NOT_SAMPLED:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.traceStartedNotSampled).getValue());
                break;
            case TRACES_JOINED_SAMPLED:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.tracesJoinedSampled).getValue());
                break;
            case TRACES_JOINED_NOT_SAMPLED:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.tracesJoinedNotSampled).getValue());
                break;
            case SPANS_STARTED_SAMPLED:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.spansStartedSampled).getValue());
                break;
            case SPANS_STARTED_NOT_SAMPLED:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.spansStartedNotSampled).getValue());
                break;
            case SPANS_FINISHED:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.spansFinished).getValue());
                break;
            case SPAN_CONTEXT_DECODING_ERRORS:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.decodingErrors).getValue());
                break;
            case REPORTER_SPANS_REPORTED_SUCCESS:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.reporterSuccess).getValue());
                break;
            case REPORTER_SPANS_REPORTED_FAILED:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.reporterFailure).getValue());
                break;
            case REPORTER_SPANS_DROPPED:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.reporterDropped).getValue());
                break;
            case REPORTER_QUEUE_LENGTH:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.reporterQueueLength).getValue());
                break;
            case SAMPLER_QUERIES_SUCCESS:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.samplerRetrieved).getValue());
                break;
            case SAMPLER_QUERIES_FAILED:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.samplerQueryFailure).getValue());
                break;
            case SAMPLER_UPDATES_SUCCESS:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.samplerUpdated).getValue());
                break;
            case SAMPLER_UPDATES_FAILED:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.samplerParsingFailure).getValue());
                break;
            case BAGGAGE_UPDATES_SUCCESS:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.baggageRestrictionsUpdateSuccess).getValue());
                break;
            case BAGGAGE_UPDATES_FAILED:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.baggageRestrictionsUpdateSuccess).getValue());
                break;
            case BAGGAGE_TRUNCATIONS:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.baggageTruncate).getValue());
                break;
            case BAGGAGE_RESTRICTIONS_UPDATES_SUCCESS:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.baggageUpdateSuccess).getValue());
                break;
            case BAGGAGE_RESTRICTIONS_UPDATES_FAILED:
                result.set(((WildflyJaegerMetricsFactory.Metric) metrics.baggageUpdateFailure).getValue());
                break;
            default:
                throw new IllegalStateException(TracingExtensionLogger.ROOT_LOGGER.unknownMetric(metric));
        }

        context.getResult().set(result);
    }

    private static ServiceName resolveDeploymentUnitServiceName(OperationContext context, ModelNode operation) {
        PathAddress address = context.getCurrentAddress();

        if (address.getElement(1).getKey().equals(ModelDescriptionConstants.SUBDEPLOYMENT)) {
            return Services.deploymentUnitName(
                    resolveRuntimeName(context, address.getElement(0)),
                    address.getElement(1).getValue()
            );
        } else {
            return Services.deploymentUnitName(
                    resolveRuntimeName(context, address.getElement(0))
            );
        }
    }

    private static String resolveRuntimeName(OperationContext context, PathElement address) {
        return context.readResourceFromRoot(PathAddress.pathAddress(address), false)
                .getModel()
                .get(ModelDescriptionConstants.RUNTIME_NAME)
                .asString();
    }
}
