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
package org.wildfly.extension.microprofile.metrics;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNIT;
import static org.wildfly.extension.microprofile.config.smallrye.ServiceNames.CONFIG_PROVIDER;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.CLIENT_FACTORY_CAPABILITY;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.MANAGEMENT_EXECUTOR;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.WILDFLY_REGISTRATION_SERVICE;
import static org.wildfly.extension.microprofile.metrics._private.MicroProfileMetricsLogger.LOGGER;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import io.smallrye.metrics.ExtendedMetadata;
import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.setup.JmxRegistrar;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class MetricsRegistrationService implements Service<MetricsRegistrationService> {

    private final ImmutableManagementResourceRegistration rootResourceRegistration;
    private final Resource rootResource;
    private final Supplier<ModelControllerClientFactory> modelControllerClientFactory;
    private final Supplier<Executor> managementExecutor;
    private final List<String> exposedSubsystems;
    private final boolean exposeAnySubsystem;
    private JmxRegistrar jmxRegistrar;
    private LocalModelControllerClient modelControllerClient;

    static void install(OperationContext context, List<String> exposedSubsystems) {
        ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
        Resource rootResource = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);

        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(WILDFLY_REGISTRATION_SERVICE);
        Supplier<ModelControllerClientFactory> modelControllerClientFactory = serviceBuilder.requires(context.getCapabilityServiceName(CLIENT_FACTORY_CAPABILITY, ModelControllerClientFactory.class));
        Supplier<Executor> managementExecutor = serviceBuilder.requires(context.getCapabilityServiceName(MANAGEMENT_EXECUTOR, Executor.class));
        serviceBuilder.requires(CONFIG_PROVIDER);
        MetricsRegistrationService service = new MetricsRegistrationService(rootResourceRegistration, rootResource, modelControllerClientFactory, managementExecutor, exposedSubsystems);
        serviceBuilder.setInstance(service)
                .install();
    }

    public MetricsRegistrationService(ImmutableManagementResourceRegistration rootResourceRegistration, Resource rootResource, Supplier<ModelControllerClientFactory> modelControllerClientFactory, Supplier<Executor> managementExecutor, List<String> exposedSubsystems) {
        this.rootResourceRegistration = rootResourceRegistration;
        this.rootResource = rootResource;
        this.modelControllerClientFactory = modelControllerClientFactory;
        this.managementExecutor = managementExecutor;
        this.exposedSubsystems = exposedSubsystems;
        this.exposeAnySubsystem = exposedSubsystems.remove("*");
    }

    @Override
    public void start(StartContext context) throws StartException {
        jmxRegistrar = new JmxRegistrar();
        try {
            jmxRegistrar.init();
        } catch (IOException e) {
            throw LOGGER.failedInitializeJMXRegistrar(e);
        }

        modelControllerClient = modelControllerClientFactory.get().createClient(managementExecutor.get());
        // register metrics from WildFly subsystems in the VENDOR metric registry
        registerMetrics(rootResource,
                rootResourceRegistration,
                MetricRegistries.get(MetricRegistry.Type.VENDOR),
                (address, attributeName) -> address.toPathStyleString().substring(1) + "/" + attributeName);
    }

    @Override
    public void stop(StopContext context) {
        for (MetricRegistry registry : new MetricRegistry[]{
                MetricRegistries.get(MetricRegistry.Type.BASE),
                MetricRegistries.get(MetricRegistry.Type.VENDOR)}) {
            for (String name : registry.getNames()) {
                registry.remove(name);
            }
        }

        modelControllerClient.close();
        jmxRegistrar = null;
    }

    @Override
    public MetricsRegistrationService getValue() {
        return this;
    }

    public interface MetricNameCreator {
        String createName(PathAddress address, String attributeName);

        default PathAddress getResourceAddress(PathAddress address) {
            return address;
        }
    }

    public Set<String> registerMetrics(Resource rootResource,
                                       ImmutableManagementResourceRegistration managementResourceRegistration,
                                       MetricRegistry metricRegistry,
                                       MetricNameCreator metricNameCreator) {
        Map<PathAddress, Map<String, ModelNode>> metrics = findMetrics(rootResource, managementResourceRegistration);
        Set<String> registeredMetrics = registerMetrics(metrics, metricRegistry, metricNameCreator);
        return registeredMetrics;
    }


    private Map<PathAddress, Map<String, ModelNode>> findMetrics(Resource rootResource, ImmutableManagementResourceRegistration managementResourceRegistration) {
        Map<PathAddress, Map<String, ModelNode>> metrics = new HashMap<>();
        collectMetrics(rootResource, managementResourceRegistration, PathAddress.EMPTY_ADDRESS, metrics);
        return metrics;
    }

    private void collectMetrics(final Resource current, ImmutableManagementResourceRegistration managementResourceRegistration, final PathAddress address, Map<PathAddress, Map<String, ModelNode>> collectedMetrics) {

        if (!isExposingMetrics(address)) {
            return;
        }

        Map<String, AttributeAccess> attributes = managementResourceRegistration.getAttributes(address);
        ModelNode description = null;
        for (Map.Entry<String, AttributeAccess> entry : attributes.entrySet()) {
            String attributeName = entry.getKey();
            AttributeAccess attributeAccess = entry.getValue();
            if (attributeAccess.getAccessType() == AttributeAccess.AccessType.METRIC
                    && attributeAccess.getStorageType() == AttributeAccess.Storage.RUNTIME) {
                if (description == null) {
                    DescriptionProvider modelDescription = managementResourceRegistration.getModelDescription(address);
                    description = modelDescription.getModelDescription(Locale.getDefault());
                }

                Map<String, ModelNode> metricsForResource = collectedMetrics.get(address);
                if (metricsForResource == null) {
                    metricsForResource = new HashMap<>();
                }
                metricsForResource.put(attributeName, description.get(ATTRIBUTES, attributeName));
                collectedMetrics.put(address, metricsForResource);
            }
        }

        for (String type : current.getChildTypes()) {
            if (current.hasChildren(type)) {
                for (Resource.ResourceEntry entry : current.getChildren(type)) {
                    final PathElement pathElement = entry.getPathElement();
                    final PathAddress childAddress = address.append(pathElement);
                    collectMetrics(entry, managementResourceRegistration, childAddress, collectedMetrics);
                }
            }
        }
    }

    public Set<String> registerMetrics(Map<PathAddress, Map<String, ModelNode>> metrics, MetricRegistry registry, MetricNameCreator metricNameCreator) {
        Set<String> registeredMetricNames = new HashSet<>();

        for (Map.Entry<PathAddress, Map<String, ModelNode>> entry : metrics.entrySet()) {
            PathAddress address = entry.getKey();
            for (Map.Entry<String, ModelNode> wildflyMetric : entry.getValue().entrySet()) {
                String attributeName = wildflyMetric.getKey();
                ModelNode attributeDescription = wildflyMetric.getValue();

                String metricName = metricNameCreator.createName(address, attributeName);
                String unit = attributeDescription.get(UNIT).asString(MetricUnits.NONE).toLowerCase();
                String description = attributeDescription.get(DESCRIPTION).asStringOrNull();

                Metadata metadata = new ExtendedMetadata(metricName, attributeName + " for " + address.toHttpStyleString(), description, MetricType.GAUGE, unit);

                ModelType type = attributeDescription.get(TYPE).asType();

                switch (type) {
                    // simple numerical type
                    case BIG_DECIMAL:
                    case BIG_INTEGER:
                    case DOUBLE:
                    case INT:
                    case LONG:
                        break;
                    case BYTES:
                    case LIST:
                    case OBJECT:
                    case PROPERTY:
                    case EXPRESSION:
                    case BOOLEAN:
                    case STRING:
                    case TYPE:
                    case UNDEFINED:
                    default:
                        LOGGER.debugf("Type %s is not supported for MicroProfile Metrics, the attribute %s on %s will not be registered.", type, attributeName, address);
                        continue;
                }
                Metric metric = new Gauge() {
                    @Override
                    public Number getValue() {
                        final ModelNode readAttributeOp = new ModelNode();
                        readAttributeOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
                        readAttributeOp.get(OP_ADDR).set(metricNameCreator.getResourceAddress(address).toModelNode());
                        readAttributeOp.get(ModelDescriptionConstants.INCLUDE_UNDEFINED_METRIC_VALUES).set(true);
                        readAttributeOp.get(NAME).set(attributeName);
                        ModelNode response = modelControllerClient.execute(readAttributeOp);
                        String error = getFailureDescription(response);
                        if (error != null) {
                            registry.remove(metricName);
                            throw LOGGER.unableToReadAttribute(attributeName, address, error);
                        }
                        ModelNode result = response.get(RESULT);
                        if (result.isDefined()) {
                            try {
                                switch (type) {
                                    case INT:
                                        return result.asInt();
                                    case LONG:
                                        return result.asLong();
                                    default:
                                        // handle other numerical types as Double
                                        return result.asDouble();
                                }
                            } catch (Exception e) {
                                throw LOGGER.unableToConvertAttribute(attributeName, address, e);
                            }
                        } else {
                            registry.remove(metricName);
                            throw LOGGER.undefinedMetric(attributeName, address);
                        }
                    }
                };
                registry.register(metadata, metric);
                registeredMetricNames.add(metadata.getName());
            }
        }
        return registeredMetricNames;
    }

    private boolean isExposingMetrics(PathAddress address) {
        // root resource
        if (address.size() == 0) {
            return true;
        }
        if (address.getElement(0).getKey().equals(SUBSYSTEM)) {
            String subsystemName = address.getElement(0).getValue();
            return exposeAnySubsystem || exposedSubsystems.contains(subsystemName);
        }
        // do not expose metrics for resources outside the subsystems.
        return false;
    }

    private boolean isMetric(AttributeAccess attributeAccess) {
        if (attributeAccess.getAccessType() == AttributeAccess.AccessType.METRIC && attributeAccess.getStorageType() == AttributeAccess.Storage.RUNTIME) {
            return true;
        }
        return false;
    }

    private String getFailureDescription(ModelNode result) {
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            return result.get(FAILURE_DESCRIPTION).toString();
        }
        return null;
    }
}