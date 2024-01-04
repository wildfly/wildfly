/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.metrics.MetricMetadata.Type.COUNTER;
import static org.wildfly.extension.metrics.MetricMetadata.Type.GAUGE;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

public class MetricCollector {
    private final LocalModelControllerClient modelControllerClient;
    private final ProcessStateNotifier processStateNotifier;

    public MetricCollector(LocalModelControllerClient modelControllerClient, ProcessStateNotifier processStateNotifier) {
        this.modelControllerClient = modelControllerClient;
        this.processStateNotifier = processStateNotifier;
    }

    // collect metrics from the resources
    public synchronized void collectResourceMetrics(final Resource resource,
                                                     ImmutableManagementResourceRegistration managementResourceRegistration,
                                                     Function<PathAddress, PathAddress> resourceAddressResolver,
                                                     boolean exposeAnySubsystem,
                                                     List<String> exposedSubsystems,
                                                     String prefix,
                                                     MetricRegistration registration) {
        collectResourceMetrics0(resource, managementResourceRegistration, EMPTY_ADDRESS, resourceAddressResolver, registration,
                exposeAnySubsystem, exposedSubsystems, prefix);
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
        Runnable cleanupTask = new Runnable() {
            @Override
            public void run() {
                processStateNotifier.removePropertyChangeListener(listener);
            }
        };
        registration.addCleanUpTask(cleanupTask);
        // If server is already running, we won't get a change event so register now
        if (ControlledProcessState.State.RUNNING == this.processStateNotifier.getCurrentState()) {
            registration.register();
        }
    }

    private void collectResourceMetrics0(final Resource current,
                                         ImmutableManagementResourceRegistration managementResourceRegistration,
                                         PathAddress address,
                                         Function<PathAddress, PathAddress> resourceAddressResolver,
                                         MetricRegistration registration, boolean exposeAnySubsystem, List<String> exposedSubsystems, String prefix) {
        if (!isExposingMetrics(address, exposeAnySubsystem, exposedSubsystems)) {
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
            String attributeDescription = resourceDescription.get(ATTRIBUTES, attributeName, DESCRIPTION).asStringOrNull();

            WildFlyMetric metric = new WildFlyMetric(modelControllerClient, resourceAddress, attributeName);
            WildFlyMetricMetadata metadata = new WildFlyMetricMetadata(attributeName, resourceAddress, prefix, attributeDescription, unit, isCounter ? COUNTER : GAUGE);

            registration.addRegistrationTask(() -> registration.registerMetric(metric, metadata));
            registration.addUnregistrationTask(metadata.getMetricID());
        }

        for (String type : current.getChildTypes()) {
            for (Resource.ResourceEntry entry : current.getChildren(type)) {
                final PathElement pathElement = entry.getPathElement();
                final PathAddress childAddress = address.append(pathElement);
                collectResourceMetrics0(entry, managementResourceRegistration, childAddress, resourceAddressResolver, registration, exposeAnySubsystem, exposedSubsystems, prefix);
            }
        }
    }

    private boolean isExposingMetrics(PathAddress address, boolean exposeAnySubsystem, List<String> exposedSubsystems) {
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
}
