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
import static org.wildfly.extension.metrics._private.MetricsLogger.LOGGER;

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
import org.jboss.as.controller.client.helpers.Operations;
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
    private ImmutableManagementResourceRegistration modelRegistration;
    private MetricRegistration modelMetrics;
    private boolean exposeAnySubsystem;
    private List<String> exposedSubsystems;
    private String prefix;

    public MetricCollector(LocalModelControllerClient modelControllerClient, ProcessStateNotifier processStateNotifier) {
        this.modelControllerClient = modelControllerClient;
        this.processStateNotifier = processStateNotifier;
    }

    public void resourceAdded(PathAddress address) {
        ImmutableManagementResourceRegistration registration;
        MetricRegistration metrics;
        boolean exposeAny;
        List<String> exposed;
        String metricPrefix;
        synchronized (this) {
            registration = modelRegistration;
            metrics = modelMetrics;
            exposeAny = exposeAnySubsystem;
            exposed = exposedSubsystems;
            metricPrefix = prefix;
        }
        if (registration == null || metrics == null) {
            return;
        }
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            ModelNode result = null;
            try {
                result = modelControllerClient.execute(Operations.createReadResourceOperation(address.toModelNode(), true));
            } catch (RuntimeException e) {
                lastFailure = e;
            }
            if (result != null && Operations.isSuccessfulOutcome(result)) {
                Resource resource = Resource.Factory.create();
                resource.writeModel(result.get("result"));
                synchronized (this) {
                    if (modelRegistration == registration && modelMetrics == metrics) {
                        registerDynamicResource(resource, registration, address, Function.identity(), metrics,
                                                exposeAny, exposed, metricPrefix);
                    }
                }
                return;
            }
            if (attempt == 9) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.debug("Interrupted while collecting metrics for resource " + address);
                return;
            }
        }
        LOGGER.unableToCollectMetrics(address, 10,
              lastFailure == null ? "" : ": " + lastFailure.getMessage());
    }

    public synchronized void resourceRemoved(PathAddress address) {
        if (modelMetrics != null) {
            modelMetrics.unregister(address);
        }
    }

    public synchronized void stop() {
        if (modelMetrics != null) {
            modelMetrics.unregister();
            modelMetrics = null;
        }
        modelRegistration = null;
        exposedSubsystems = null;
    }

    public synchronized void collectRootResourceMetrics(Resource resource,
                                                        ImmutableManagementResourceRegistration registration,
                                                        boolean exposeAnySubsystem,
                                                        List<String> exposedSubsystems,
                                                        String prefix,
                                                        MetricRegistration metricRegistration) {
        modelRegistration = registration;
        modelMetrics = metricRegistration;
        this.exposeAnySubsystem = exposeAnySubsystem;
        this.exposedSubsystems = List.copyOf(exposedSubsystems);
        this.prefix = prefix;
        registerResourceMetrics(resource, registration, Function.identity(), this.exposeAnySubsystem,
                                this.exposedSubsystems, this.prefix, metricRegistration);
    }

    // collect metrics from the resources
    public synchronized void registerResourceMetrics(final Resource resource,
                                                     ImmutableManagementResourceRegistration managementResourceRegistration,
                                                     Function<PathAddress, PathAddress> resourceAddressResolver,
                                                     boolean exposeAnySubsystem,
                                                     List<String> exposedSubsystems,
                                                     String prefix,
                                                     MetricRegistration registration) {
        createRegistrationTasks(resource, managementResourceRegistration, EMPTY_ADDRESS, resourceAddressResolver, registration,
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

    private synchronized void registerDynamicResource(final Resource resource,
                                                      ImmutableManagementResourceRegistration managementResourceRegistration,
                                                      PathAddress address,
                                                      Function<PathAddress, PathAddress> resourceAddressResolver,
                                                      MetricRegistration registration,
                                                      boolean exposeAnySubsystem,
                                                      List<String> exposedSubsystems,
                                                      String prefix) {
        createRegistrationTasks(resource, managementResourceRegistration, address, resourceAddressResolver, registration,
                                exposeAnySubsystem, exposedSubsystems, prefix);
        registration.register();
    }

    private void createRegistrationTasks(final Resource current,
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
        }

        for (String type : current.getChildTypes()) {
            for (Resource.ResourceEntry entry : current.getChildren(type)) {
                final PathElement pathElement = entry.getPathElement();
                final PathAddress childAddress = address.append(pathElement);
                createRegistrationTasks(entry, managementResourceRegistration, childAddress, resourceAddressResolver, registration, exposeAnySubsystem, exposedSubsystems, prefix);
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
