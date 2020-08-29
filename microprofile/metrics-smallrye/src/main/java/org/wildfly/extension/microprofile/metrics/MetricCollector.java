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
import static org.wildfly.extension.microprofile.metrics._private.MicroProfileMetricsLogger.LOGGER;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.smallrye.metrics.ExtendedMetadata;
import io.smallrye.metrics.MetricRegistries;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class MetricCollector {


    private final boolean exposeAnySubsystem;
    private String globalPrefix;
    private final List<String> exposedSubsystems;
    private final LocalModelControllerClient modelControllerClient;
    private final ProcessStateNotifier processStateNotifier;

    public MetricCollector(LocalModelControllerClient modelControllerClient, ProcessStateNotifier processStateNotifier, List<String> exposedSubsystems, String globalPrefix) {
        this.modelControllerClient = modelControllerClient;
        this.processStateNotifier = processStateNotifier;
        this.exposedSubsystems = exposedSubsystems;
        this.exposeAnySubsystem = exposedSubsystems.remove("*");
        this.globalPrefix = globalPrefix;
    }

    // collect metrics from the resources
    public MetricRegistration collectResourceMetrics(final Resource resource,
                                                     ImmutableManagementResourceRegistration managementResourceRegistration,
                                                     Function<PathAddress, PathAddress> resourceAddressResolver) {
        MetricRegistration registration = new MetricRegistration();
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

            Tag[] tags = createTags(metricMetadata);
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

    private void registerMetric(MetricMetadata metricMetadata, PathAddress address, String attributeName, MeasurementUnit unit, String description, boolean isCounter, Tag[] tags) {
        final Metric metric;
        if (isCounter) {
            metric = new Counter() {
                @Override
                public void inc() {
                }

                @Override
                public void inc(long n) {
                }

                @Override
                public long getCount() {
                    ModelNode result = readAttributeValue(address, attributeName);
                    if (result.isDefined()) {
                        try {
                            return result.asLong();
                        } catch (Exception e) {
                            throw LOGGER.unableToConvertAttribute(attributeName, address, e);
                        }
                    }
                    return 0;
                }
            };
        } else {
            metric = new Gauge<Number>() {
                @Override
                public Double getValue() {
                    ModelNode result = readAttributeValue(address, attributeName);
                    if (result.isDefined()) {
                        try {
                            return result.asDouble();
                        } catch (Exception e) {
                            throw LOGGER.unableToConvertAttribute(attributeName, address, e);
                        }
                    }
                    return 0.0;
                }
            };
        }
        final Metadata metadata;
        MetricRegistry vendorRegistry = MetricRegistries.get(MetricRegistry.Type.VENDOR);
        synchronized (vendorRegistry) {
            Metadata existingMetadata = vendorRegistry.getMetadata().get(metricMetadata.metricName);
            if (existingMetadata != null) {
                metadata = existingMetadata;
            } else {
                metadata = new ExtendedMetadata(metricMetadata.metricName, metricMetadata.metricName, description,
                        isCounter ? MetricType.COUNTER : MetricType.GAUGE, metricUnit(unit),
                        null, false,
                        // for WildFly subsystem metrics, the microprofile scope is put in the OpenMetrics tags
                        // so that the name of the metric does not change ("vendor_" will not be prepended to it).
                        Optional.of(false));
            }
            vendorRegistry.register(metadata, metric, tags);
        }
    }

    private String metricUnit(MeasurementUnit unit) {
        if (unit == null) {
            return MetricUnits.NONE;
        }
        switch (unit) {

            case PERCENTAGE:
                return MetricUnits.PERCENT;
            case BYTES:
                return MetricUnits.BYTES;
            case KILOBYTES:
                return MetricUnits.KILOBYTES;
            case MEGABYTES:
                return MetricUnits.MEGABYTES;
            case GIGABYTES:
                return MetricUnits.GIGABYTES;
            case TERABYTES:
                return "terabytes";
            case PETABYTES:
                return "petabytes";
            case BITS:
                return MetricUnits.BITS;
            case KILOBITS:
                return MetricUnits.KILOBITS;
            case MEGABITS:
                return MetricUnits.MEBIBITS;
            case GIGABITS:
                return MetricUnits.GIGABITS;
            case TERABITS:
                return "terabits";
            case PETABITS:
                return "petabits";
            case EPOCH_MILLISECONDS:
                return MetricUnits.MILLISECONDS;
            case EPOCH_SECONDS:
                return MetricUnits.SECONDS;
            case JIFFYS:
                return "jiffys";
            case NANOSECONDS:
                return MetricUnits.NANOSECONDS;
            case MICROSECONDS:
                return MetricUnits.MICROSECONDS;
            case MILLISECONDS:
                return MetricUnits.MILLISECONDS;
            case SECONDS:
                return MetricUnits.SECONDS;
            case MINUTES:
                return MetricUnits.MINUTES;
            case HOURS:
                return MetricUnits.HOURS;
            case DAYS:
                return MetricUnits.DAYS;
            case PER_JIFFY:
                return "per-jiffy";
            case PER_NANOSECOND:
                return "per_nanoseconds";
            case PER_MICROSECOND:
                return "per_microseconds";
            case PER_MILLISECOND:
                return "per_milliseconds";
            case PER_SECOND:
                return MetricUnits.PER_SECOND;
            case PER_MINUTE:
                return "per_minutes";
            case PER_HOUR:
                return "per_hour";
            case PER_DAY:
                return "per_day";
            case CELSIUS:
                return "degree_celsius";
            case KELVIN:
                return "kelvin";
            case FAHRENHEIGHT:
                return "degree_fahrenheit";
            case NONE:
                default:
                    return "none";
        }
    }

    private Tag[] createTags(MetricMetadata metadata) {
        Tag[] tags = new Tag[metadata.labelNames.size()];
        for (int i = 0; i < metadata.labelNames.size(); i++) {
            String name = metadata.labelNames.get(i);
            String value = metadata.labelValues.get(i);
            tags[i] = new Tag(name, value);
        }
        return tags;
    }

    private ModelNode readAttributeValue(PathAddress address, String attributeName) {
        final ModelNode readAttributeOp = new ModelNode();
        readAttributeOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readAttributeOp.get(OP_ADDR).set(address.toModelNode());
        readAttributeOp.get(ModelDescriptionConstants.INCLUDE_UNDEFINED_METRIC_VALUES).set(true);
        readAttributeOp.get(NAME).set(attributeName);
        ModelNode response = modelControllerClient.execute(readAttributeOp);
        String error = getFailureDescription(response);
        if (error != null) {
            throw LOGGER.unableToReadAttribute(attributeName, address, error);
        }
        return  response.get(RESULT);
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

    private String getFailureDescription(ModelNode result) {
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            return result.get(FAILURE_DESCRIPTION).toString();
        }
        return null;
    }

    public static final class MetricRegistration {

        private final List<Runnable> registrationTasks = new ArrayList<>();
        private final List<MetricID> unregistrationTasks = new ArrayList<>();

        MetricRegistration() {
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
            MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.VENDOR);
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

        private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("(?<=[a-z])[A-Z]");

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

        private static String getPrometheusMetricName(String name) {
            name =name.replaceAll("[^\\w]+","_");
            name = decamelize(name);
            return name;
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
