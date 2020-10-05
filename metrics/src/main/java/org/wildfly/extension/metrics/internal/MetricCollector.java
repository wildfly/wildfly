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
package org.wildfly.extension.metrics.internal;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.metrics.internal.PrometheusExporter.getPrometheusMetricName;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.metrics.internal.WildFlyMetricRegistry.MetricID;

public class MetricCollector {


    private final boolean exposeAnySubsystem;
    private String globalPrefix;
    private final List<String> exposedSubsystems;
    private final LocalModelControllerClient modelControllerClient;
    private final ProcessStateNotifier processStateNotifier;
    private final WildFlyMetricRegistry wildFlyMetricRegistry;

    public MetricCollector(LocalModelControllerClient modelControllerClient, ProcessStateNotifier processStateNotifier, WildFlyMetricRegistry wildFlyMetricRegistry, List<String> exposedSubsystems, String globalPrefix) {
        this.modelControllerClient = modelControllerClient;
        this.processStateNotifier = processStateNotifier;
        this.wildFlyMetricRegistry = wildFlyMetricRegistry;
        this.exposedSubsystems = exposedSubsystems;
        this.exposeAnySubsystem = exposedSubsystems.remove("*");
        this.globalPrefix = globalPrefix;
    }

    // collect metrics from the resources
    public MetricRegistration collectResourceMetrics(final Resource resource,
                                                     ImmutableManagementResourceRegistration managementResourceRegistration,
                                                     Function<PathAddress, PathAddress> resourceAddressResolver) {
        MetricRegistration registration = new MetricRegistration(wildFlyMetricRegistry);
        collectResourceMetrics0(resource, managementResourceRegistration, EMPTY_ADDRESS, resourceAddressResolver, registration);
        // Defer the actual registration until the server is running and they can be collected w/o errors
        PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (ControlledProcessState.State.RUNNING == evt.getNewValue()) {
                    registration.register();
                } else if (ControlledProcessState.State.STOPPING == evt.getNewValue()) {
                    // Unregister so if this is a reload they won't still be around in a static cache in MetricsRegistry
                    // and cause problems when the server is starting
                    registration.unregister();
                    processStateNotifier.removePropertyChangeListener(this);
                }

            }
        };
        this.processStateNotifier.addPropertyChangeListener(listener);
        // If server is already running, we won't get a change event so register now
        if (ControlledProcessState.State.RUNNING == this.processStateNotifier.getCurrentState()) {
            registration.register();
        }
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
            boolean isCounter = attributeAccess.getFlags().contains(AttributeAccess.Flag.COUNTER_METRIC);
            MetricMetadata metricMetadata = new MetricMetadata(attributeName, resourceAddress, globalPrefix);
            String attributeDescription = resourceDescription.get(ATTRIBUTES, attributeName, DESCRIPTION).asStringOrNull();

            MetricTag[] tags = createTags(metricMetadata);
            MetricID metricID = new MetricID(metricMetadata.metricName, tags);

            registration.addRegistrationTask(() -> registerMetric(metricMetadata, resourceAddress, attributeName, unit, attributeDescription, isCounter, tags));
            registration.addUnregistrationTask(metricID);
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

    private void registerMetric(MetricMetadata metricMetadata, PathAddress address, String attributeName, MeasurementUnit unit, String description, boolean isCounter, MetricTag[] tags) {
        final WildFlyMetric metric = new WildFlyMetric(modelControllerClient, address, attributeName);
        final WildFlyMetricMetadata metadata = new WildFlyMetricMetadata(metricMetadata.metricName, description, unit, isCounter? WildFlyMetricMetadata.Type.COUNTER : WildFlyMetricMetadata.Type.GAUGE);
        synchronized (wildFlyMetricRegistry) {
            wildFlyMetricRegistry.register(metadata, metric, tags);
        }
    }

    private MetricTag[] createTags(MetricMetadata metadata) {
        MetricTag[] tags = new MetricTag[metadata.labelNames.size()];
        for (int i = 0; i < metadata.labelNames.size(); i++) {
            String name = metadata.labelNames.get(i);
            String value = metadata.labelValues.get(i);
            tags[i] = new MetricTag(name, value);
        }
        return tags;
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

    public static final class MetricRegistration {

        private final List<Runnable> registrationTasks = new ArrayList<>();
        private final List<MetricID> unregistrationTasks = new ArrayList<>();
        private final WildFlyMetricRegistry registry;

        MetricRegistration(WildFlyMetricRegistry registry) {
            this.registry = registry;
        }

        public synchronized void register() { // synchronized to avoid registering same thing twice. Shouldn't really be possible; just being cautious
            for (Runnable task : registrationTasks) {
                task.run();
            }
            // This object will last until undeploy or server stop,
            // so clean up and save memory
            registrationTasks.clear();
        }

        public void unregister() {
            for (MetricID id : unregistrationTasks) {
                registry.remove(id);
            }
        }

        private synchronized void addRegistrationTask(Runnable task) {
            registrationTasks.add(task);
        }

        private void addUnregistrationTask(MetricID metricID) {
            unregistrationTasks.add(metricID);
        }
    }

    private static class MetricMetadata {


        private final String metricName;
        private final List<String> labelNames;
        private final List<String> labelValues;

        MetricMetadata(String attributeName, PathAddress address, String globalPrefix) {
            String metricPrefix = "";
            labelNames = new ArrayList<>();
            labelValues = new ArrayList<>();
            for (PathElement element: address) {
                String key = element.getKey();
                String value = element.getValue();
                // prepend the subsystem or statistics name to the attribute
                if (key.equals(SUBSYSTEM) || key.equals("statistics")) {
                    metricPrefix += value + "-";
                    continue;
                }
                labelNames.add(getPrometheusMetricName(key));
                labelValues.add(value);
            }
            // if the resource address defines a deployment (without subdeployment),
            if (labelNames.contains(DEPLOYMENT)  && !labelNames.contains(SUBDEPLOYMENT)) {
                labelNames.add(SUBDEPLOYMENT);
                labelValues.add(labelValues.get(labelNames.indexOf(DEPLOYMENT)));
            }
            if (globalPrefix != null && !globalPrefix.isEmpty()) {
                metricPrefix = globalPrefix + "-" + metricPrefix;
            }
            metricName = getPrometheusMetricName(metricPrefix + attributeName);
        }
    }

    public static class MetricTag {
        private String key;
        private String value;

        public MetricTag(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
