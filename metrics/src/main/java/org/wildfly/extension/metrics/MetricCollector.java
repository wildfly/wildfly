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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/** Collects management resource metrics using subsystem exposure fixed at construction. */
public class MetricCollector {
    private final LocalModelControllerClient valueReadClient;
    private final LocalModelControllerClient discoveryClient;
    private final ProcessStateNotifier processStateNotifier;
    private final ResourceMetricsRetryQueue resourceMetricsRetryQueue;
    private ImmutableManagementResourceRegistration modelRegistration;
    private MetricRegistration modelMetrics;
    private final boolean exposeAnySubsystem;
    private final List<String> exposedSubsystems;
    private String prefix;
    private final Map<PathAddress, RuntimeException> lastFailures = new ConcurrentHashMap<>();

    /**
     * Creates a collector with a snapshot of subsystem exposure so callers cannot change it later.
     *
     * @param valueReadClient the client used to read metric values under the caller's identity
     * @param discoveryClient the client used for asynchronous resource discovery
     * @param processStateNotifier the server process state notifier
     * @param executor the executor used for asynchronous resource discovery
     * @param exposeAnySubsystem whether metrics from every subsystem are exposed
     * @param exposedSubsystems the subsystems whose metrics are exposed when wildcard exposure is disabled
     */
    public MetricCollector(LocalModelControllerClient valueReadClient,
            LocalModelControllerClient discoveryClient,
            ProcessStateNotifier processStateNotifier,
            Executor executor,
            boolean exposeAnySubsystem,
            List<String> exposedSubsystems) {
        this.valueReadClient = valueReadClient;
        this.discoveryClient = discoveryClient;
        this.processStateNotifier = processStateNotifier;
        this.exposeAnySubsystem = exposeAnySubsystem;
        this.exposedSubsystems = List.copyOf(exposedSubsystems);

        this.resourceMetricsRetryQueue = new ResourceMetricsRetryQueue(executor,
            this::processPendingResource,
            this::unableToCollectMetrics);
    }

    /**
     * Queues a newly added management resource for asynchronous metric collection.
     *
     * @param address the address of the added resource
     */
    public synchronized void resourceAdded(PathAddress address) {
        if (modelRegistration == null || modelMetrics == null || !isExposingMetrics(address)) {
            return;
        }
        resourceMetricsRetryQueue.add(address);
    }

    /**
     * Removes metrics and pending collection work for a removed management resource.
     *
     * @param address the address of the removed resource
     */
    public synchronized void resourceRemoved(PathAddress address) {
        resourceMetricsRetryQueue.remove(address);
        lastFailures.remove(address);
        if (modelMetrics != null) {
            modelMetrics.unregister(address);
        }
    }

    /**
     * Stops asynchronous collection and unregisters all model metrics.
     */
    public synchronized void stop() {
        resourceMetricsRetryQueue.stop();
        lastFailures.clear();
        if (modelMetrics != null) {
            modelMetrics.unregister();
            modelMetrics = null;
        }
        modelRegistration = null;
    }

    /**
     * Attempts one read and registration for a queued resource.
     *
     * @param pendingResource the queued resource generation
     * @param attempt the one-based attempt number
     * @return {@code true} when processing is complete, otherwise retry
     */
    private boolean processPendingResource(ResourceMetricsRetryQueue.PendingResource pendingResource, int attempt) {
        PathAddress address = pendingResource.address();
        ImmutableManagementResourceRegistration registration;
        MetricRegistration metrics;
        String metricPrefix;
        synchronized (this) {
            registration = modelRegistration;
            metrics = modelMetrics;
            metricPrefix = prefix;
        }
        if (registration == null || metrics == null) {
            return true;
        }
        ModelNode result = null;
        try {
            result = discoveryClient.execute(Operations.createReadResourceOperation(address.toModelNode(), true));
        } catch (RuntimeException e) {
            synchronized (this) {
                if (resourceMetricsRetryQueue.isPending(pendingResource)) {
                    lastFailures.put(address, e);
                }
            }
        }
        if (result != null && Operations.isSuccessfulOutcome(result)) {
            Resource resource = Resource.Factory.create();
            resource.writeModel(result.get("result"));
            synchronized (this) {
                if (System.nanoTime() < pendingResource.deadline()
                        && resourceMetricsRetryQueue.isPending(pendingResource)
                        && modelRegistration == registration && modelMetrics == metrics) {
                    registerDynamicResource(resource, registration, address, Function.identity(), metrics, metricPrefix);
                    lastFailures.remove(address);
                    return true;
                }
                if (resourceMetricsRetryQueue.isPending(pendingResource)) {
                    lastFailures.remove(address);
                }
            }
        }
        return false;
    }

    /**
     * Logs the final failure after an address has exhausted its retry deadline.
     *
     * @param address the resource address that could not be read
     * @param attempts the number of attempts made
     */
    private synchronized void unableToCollectMetrics(PathAddress address, int attempts) {
        RuntimeException lastFailure = lastFailures.remove(address);
        LOGGER.unableToCollectMetrics(address, attempts,
                lastFailure == null ? "" : ": " + lastFailure.getMessage());
    }

    /**
     * Registers root resource metrics and retains the registration for resources added at runtime.
     *
     * @param resource the root management resource
     * @param registration the root resource registration used to discover metrics
     * @param prefix the prefix applied to metric names
     * @param metricRegistration the registration that receives discovered metrics
     */
    public synchronized void collectRootResourceMetrics(Resource resource,
                                                        ImmutableManagementResourceRegistration registration,
                                                        String prefix,
                                                        MetricRegistration metricRegistration) {
        modelRegistration = registration;
        modelMetrics = metricRegistration;
        this.prefix = prefix;
        registerResourceMetrics(resource, registration, Function.identity(), this.prefix, metricRegistration);
    }

    // collect metrics from the resources
    public synchronized void registerResourceMetrics(final Resource resource,
                                                     ImmutableManagementResourceRegistration managementResourceRegistration,
                                                     Function<PathAddress, PathAddress> resourceAddressResolver,
                                                     String prefix,
                                                     MetricRegistration registration) {
        createRegistrationTasks(resource, managementResourceRegistration, EMPTY_ADDRESS, resourceAddressResolver, registration, prefix);
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
                                                      String prefix) {
        createRegistrationTasks(resource, managementResourceRegistration, address, resourceAddressResolver, registration, prefix);
        registration.register();
    }

    private void createRegistrationTasks(final Resource current,
                                         ImmutableManagementResourceRegistration managementResourceRegistration,
                                         PathAddress address,
                                         Function<PathAddress, PathAddress> resourceAddressResolver,
                                         MetricRegistration registration,
                                         String prefix) {
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
            String attributeDescription = resourceDescription.get(ATTRIBUTES, attributeName, DESCRIPTION).asStringOrNull();

            WildFlyMetric metric = new WildFlyMetric(valueReadClient, resourceAddress, attributeName);
            WildFlyMetricMetadata metadata = new WildFlyMetricMetadata(attributeName, resourceAddress, prefix, attributeDescription, unit, isCounter ? COUNTER : GAUGE);

            registration.addRegistrationTask(() -> registration.registerMetric(metric, metadata));
        }

        for (String type : current.getChildTypes()) {
            for (Resource.ResourceEntry entry : current.getChildren(type)) {
                final PathElement pathElement = entry.getPathElement();
                final PathAddress childAddress = address.append(pathElement);
                createRegistrationTasks(entry, managementResourceRegistration, childAddress, resourceAddressResolver, registration, prefix);
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
