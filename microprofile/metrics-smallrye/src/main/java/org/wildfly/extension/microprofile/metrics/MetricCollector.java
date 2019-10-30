/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.microprofile.metrics;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.microprofile.metrics.UnitConverter.unitSuffix;
import static org.wildfly.extension.microprofile.metrics._private.MicroProfileMetricsLogger.LOGGER;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class MetricCollector {


    private final PrometheusCollector prometheusCollector;
    private final boolean exposeAnySubsystem;
    private String globalPrefix;
    private final List<String> exposedSubsystems;
    private final LocalModelControllerClient modelControllerClient;

    public MetricCollector(LocalModelControllerClient modelControllerClient, List<String> exposedSubsystems, String globalPrefix) {
        this.modelControllerClient = modelControllerClient;
        this.exposedSubsystems = exposedSubsystems;
        this.exposeAnySubsystem = exposedSubsystems.remove("*");
        this.globalPrefix = globalPrefix;
        this.prometheusCollector = new PrometheusCollector();
        this.prometheusCollector.register(CollectorRegistry.defaultRegistry);
    }

    void close() {
        CollectorRegistry.defaultRegistry.unregister(prometheusCollector);
    }

    // collect metrics from the resources
    public MetricRegistration collectResourceMetrics(final Resource resource,
                                              ImmutableManagementResourceRegistration managementResourceRegistration,
                                              Function<PathAddress, PathAddress> resourceAddressResolver) {
        MetricRegistration registration = new MetricRegistration();
        collectResourceMetrics0(resource, managementResourceRegistration, EMPTY_ADDRESS, resourceAddressResolver, registration);
        return registration;
    }

    private void collectResourceMetrics0(final Resource current,
                                        ImmutableManagementResourceRegistration managementResourceRegistration,
                                         PathAddress address,
                                         Function<PathAddress, PathAddress> resourceAddressResolver,
                                        MetricRegistration registration) {
        if (!isExposingMetrics(address)) {
            return;
        }

        Map<String, AttributeAccess> attributes = managementResourceRegistration.getAttributes(address);
        if (attributes == null) {
            return;
        }

        ModelNode resourceDescription = null;
        for (Map.Entry<String, AttributeAccess> entry : attributes.entrySet()) {
            String attributeName = entry.getKey();

            AttributeAccess attributeAccess = entry.getValue();
            if (!isCollectibleMetric(attributeAccess)) {
                continue;
            }

            if (resourceDescription == null) {
                DescriptionProvider modelDescription = managementResourceRegistration.getModelDescription(address);
                resourceDescription = modelDescription.getModelDescription(Locale.getDefault());
            }
            PathAddress resourceAddress = resourceAddressResolver.apply(address);
            MeasurementUnit unit = attributeAccess.getAttributeDefinition().getMeasurementUnit();
            boolean counter = attributeAccess.getFlags().contains(AttributeAccess.Flag.COUNTER_METRIC);
            MetricMetadata metricMetadata = new MetricMetadata(attributeName, resourceAddress, unit, globalPrefix, counter);
            String attributeDescription = resourceDescription.get(ATTRIBUTES, attributeName, DESCRIPTION).asStringOrNull();
            final MetricFamilySamples metricFamilySamples;
            if (counter) {
                metricFamilySamples = new CounterMetricFamily(metricMetadata.metricName, attributeDescription, metricMetadata.labelNames);
            } else {
                metricFamilySamples = new GaugeMetricFamily(metricMetadata.metricName, attributeDescription, metricMetadata.labelNames);
            }
            Supplier<Optional<MetricFamilySamples.Sample>> sampleSupplier = () -> {
                final ModelNode readAttributeOp = new ModelNode();
                readAttributeOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
                readAttributeOp.get(OP_ADDR).set(resourceAddress.toModelNode());
                readAttributeOp.get(ModelDescriptionConstants.INCLUDE_UNDEFINED_METRIC_VALUES).set(true);
                readAttributeOp.get(NAME).set(attributeName);
                ModelNode response = modelControllerClient.execute(readAttributeOp);
                String error = getFailureDescription(response);
                if (error != null) {
                    throw LOGGER.unableToReadAttribute(attributeName, address, error);
                }
                ModelNode result = response.get(RESULT);
                if (result.isDefined()) {
                    try {
                        double initialValue = result.asDouble();
                        double scaledValue = UnitConverter.scaleToBase(initialValue, unit);
                        return Optional.of(new MetricFamilySamples.Sample(metricMetadata.metricName, metricMetadata.labelNames, metricMetadata.labelValues, scaledValue));
                    } catch (Exception e) {
                        throw LOGGER.unableToConvertAttribute(attributeName, address, e);
                    }
                }
                return Optional.empty();
            };
            prometheusCollector.addMetricFamilySampleSupplier(metricFamilySamples, sampleSupplier);
            registration.addUnregistrationTask(() -> prometheusCollector.removeMetricFamilySampleSupplier(metricMetadata.metricName, sampleSupplier));
        }

        for (String type : current.getChildTypes()) {
            if (current.hasChildren(type)) {
                for (Resource.ResourceEntry entry : current.getChildren(type)) {
                    final PathElement pathElement = entry.getPathElement();
                    final PathAddress childAddress = address.append(pathElement);
                    collectResourceMetrics0(entry, managementResourceRegistration, childAddress, resourceAddressResolver, registration);
                }
            }
        }
    }

    private boolean isExposingMetrics(PathAddress address) {
        // root resource
        if (address.size() == 0) {
            return true;
        }
        String subsystemName = getSubsystemName(address);
        if (subsystemName != null) {
            return exposeAnySubsystem || exposedSubsystems.contains(subsystemName);
        }
        // do not expose metrics for resources outside the subsystems and deployments.
        return false;
    }

    private String getSubsystemName(PathAddress address) {
        if (address.size() == 0) {
            return null;
        }
        if (address.getElement(0).getKey().equals(SUBSYSTEM)) {
            return address.getElement(0).getValue();
        } else {
            return getSubsystemName(address.subAddress(1));
        }
    }

    void collectMetricFamilies(ImmutableManagementResourceRegistration managementResourceRegistration,
                                       final PathAddress address) {
        if (!isExposingMetrics(address)) {
            return;
        }

        ModelNode resourceDescription = null;
        Map<String, AttributeAccess> attributes = managementResourceRegistration.getAttributes(address);
        for (Map.Entry<String, AttributeAccess> entry : attributes.entrySet()) {
            String attributeName = entry.getKey();
            AttributeAccess attributeAccess = entry.getValue();
            if (!isCollectibleMetric(attributeAccess)) {
                LOGGER.debugf("Type %s is not supported for MicroProfile Metrics, the attribute %s on %s will not be registered.",
                        attributeAccess.getAttributeDefinition().getType(), attributeName, address);
                continue;
            }
            if (resourceDescription == null) {
                DescriptionProvider modelDescription = managementResourceRegistration.getModelDescription(address);
                resourceDescription = modelDescription.getModelDescription(Locale.getDefault());
            }
            boolean counter = attributeAccess.getFlags().contains(AttributeAccess.Flag.COUNTER_METRIC);
            MetricMetadata metricMetadata = new MetricMetadata(attributeName, address, attributeAccess.getAttributeDefinition().getMeasurementUnit(), globalPrefix, counter);
            String attributeDescription = resourceDescription.get(ATTRIBUTES, attributeName, DESCRIPTION).asStringOrNull();
            final MetricFamilySamples metricFamilySamples;
            if (counter) {
                metricFamilySamples = new CounterMetricFamily(metricMetadata.metricName, attributeDescription, metricMetadata.labelNames);
            } else {
                metricFamilySamples = new GaugeMetricFamily(metricMetadata.metricName, attributeDescription, metricMetadata.labelNames);
            }
            prometheusCollector.addMetricFamilySamples(metricFamilySamples);
        }

        for (PathElement childAddress : managementResourceRegistration.getChildAddresses(address)) {
            collectMetricFamilies(managementResourceRegistration, address.append(childAddress));
        }
    }

    private boolean isCollectibleMetric(AttributeAccess attributeAccess) {
        if (attributeAccess.getAccessType() == AttributeAccess.AccessType.METRIC
                && attributeAccess.getStorageType() == AttributeAccess.Storage.RUNTIME) {
            // handle only metrics with simple numerical types
            ModelType type = attributeAccess.getAttributeDefinition().getType();
            if (type == ModelType.INT ||
                    type == ModelType.LONG ||
                    type == ModelType.DOUBLE) {
                return true;
            }
        }
        return false;
    }

    private String getFailureDescription(ModelNode result) {
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            return result.get(FAILURE_DESCRIPTION).toString();
        }
        return null;
    }

    public static final class MetricRegistration {

        private final List<Runnable> unregistrationTasks = new ArrayList<>();

        MetricRegistration() {
        }

        public void unregister() {
            for (Runnable task : unregistrationTasks) {
                task.run();
            }
        }

        private void addUnregistrationTask(Runnable task) {
            unregistrationTasks.add(task);
        }
    }

    private static class MetricMetadata {

        private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("(?<=[a-z])[A-Z]");

        private final String metricName;
        private final List<String> labelNames;
        private final List<String> labelValues;

        MetricMetadata(String attributeName, PathAddress address, MeasurementUnit unit, String globalPrefix, boolean counter) {
            String metricPrefix = "";
            labelNames = new ArrayList<>();
            labelValues = new ArrayList<>();
            for (PathElement element: address) {
                String key = element.getKey();
                String value = element.getValue();
                // prepend the subsystem or statistics name to the attribute
                if (key.equals(SUBSYSTEM) || key.equals("statistics")) {
                    metricPrefix += value + "_";
                    continue;
                }
                labelNames.add(getPrometheusMetricName(key, null, false));
                labelValues.add(value);
            }
            // if the resource address defines a deployment (without subdeployment),
            if (labelNames.contains(DEPLOYMENT)  && !labelNames.contains(SUBDEPLOYMENT)) {
                labelNames.add(SUBDEPLOYMENT);
                labelValues.add(labelValues.get(labelNames.indexOf(DEPLOYMENT)));
            }
            if (globalPrefix != null && !globalPrefix.isEmpty()) {
                metricPrefix = globalPrefix + "_" + metricPrefix;
            }
            metricName = getPrometheusMetricName(metricPrefix + attributeName, unit, counter);
        }

        private static String getPrometheusMetricName(String name, MeasurementUnit unit, boolean counter) {
            String prometheusName = name + unitSuffix(unit) + (counter ? "_total" : "");
            prometheusName =prometheusName.replaceAll("[^\\w]+","_");
            prometheusName = decamelize(prometheusName);
            return prometheusName;
        }


        private static String decamelize(String in) {
            Matcher m = SNAKE_CASE_PATTERN.matcher(in);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(sb, "_" + m.group().toLowerCase());
            }
            m.appendTail(sb);
            return sb.toString().toLowerCase();
        }
    }
}
