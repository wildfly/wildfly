/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.micrometer.jmx;

import static org.jboss.as.controller.client.helpers.MeasurementUnit.NONE;
import static org.wildfly.common.Assert.checkNotNullParam;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.wildfly.extension.micrometer.MicrometerExtensionLogger;
import org.wildfly.extension.micrometer.metrics.MetricMetadata;
import org.wildfly.extension.micrometer.metrics.WildFlyMetric;
import org.wildfly.extension.micrometer.metrics.WildFlyRegistry;

public class JmxMicrometerCollector {
    private final MBeanServer mbs;
    private WildFlyRegistry registry;

    public JmxMicrometerCollector(WildFlyRegistry registry) {
        this.registry = registry;
        this.mbs = ManagementFactory.getPlatformMBeanServer();
    }

    public void init() throws IOException {
        register("jmx-metrics.properties", registry);
    }

    private void register(String propertiesFile, WildFlyRegistry registry) throws IOException {
        List<JmxMetricMetadata> configs = findMetadata(propertiesFile);

        // expand multi mbeans
        List<JmxMetricMetadata> expandedConfigs = new ArrayList<>();
        Iterator<JmxMetricMetadata> iterator = configs.iterator();
        while (iterator.hasNext()) {
            JmxMetricMetadata metadata =  iterator.next();
            if (metadata.getTagsToFill().isEmpty()) {
                continue;
            }
            try {
                String[] split = metadata.getMBean().split("/");
                String query = split[0];
                String attribute = split[1];
                Set<ObjectName> objectNames = mbs.queryNames(ObjectName.getInstance(query), null);
                for (ObjectName objectName : objectNames) {
                    // fill the tags from the object names fields
                    List<MetricMetadata.MetricTag> tags = new ArrayList<>(metadata.getTagsToFill().size());
                    for (String key : metadata.getTagsToFill()) {
                        String value = objectName.getKeyProperty(key);
                        tags.add(new MetricMetadata.MetricTag(key, value));
                    }
                    expandedConfigs.add(new JmxMetricMetadata(metadata.getMetricName(),
                            metadata.getDescription(),
                            metadata.getMeasurementUnit(),
                            metadata.getType(),
                            objectName.getCanonicalName() + "/" + attribute,
                            Collections.emptyList(),
                            tags));

                }
                // now, it has been expanded, remove the "multi" mbean
                iterator.remove();
            } catch (MalformedObjectNameException e) {
                MicrometerExtensionLogger.MICROMETER_LOGGER.malformedName(e);
            }
        }
        configs.addAll(expandedConfigs);

        for (JmxMetricMetadata config : configs) {
            register(registry, config);
        }
    }

    void register(WildFlyRegistry registry, JmxMetricMetadata metadata) {
        WildFlyMetric metric = new WildFlyMetric() {
            @Override
            public OptionalDouble getValue() {
                try {
                    return getValueFromMBean(mbs, metadata.getMBean());
                } catch (Exception e) {
                    return OptionalDouble.empty();
                }
            }
        };
        registry.addMeter(metric, metadata);
    }

    private List<JmxMetricMetadata> findMetadata(String propertiesFile) throws IOException {
        try (
                InputStream propertiesResource = getResource(propertiesFile)) {
            if (propertiesResource == null) {
                return Collections.emptyList();
            }

            return loadMetadataFromProperties(propertiesResource);
        }
    }

    List<JmxMetricMetadata> loadMetadataFromProperties(InputStream propertiesResource) throws IOException {
        Properties props = new Properties();
        props.load(propertiesResource);

        Map<String, List<MetricProperty>> parsedMetrics = props.entrySet()
                .stream()
                .map(MetricProperty::new)
                .collect(Collectors.groupingBy(MetricProperty::getMetricName));

        return parsedMetrics.entrySet()
                .stream()
                .map(this::metadataOf)
                .sorted(Comparator.comparing(e -> e.getMetricName()))
                .collect(Collectors.toList());
    }

    private InputStream getResource(String location) {
        InputStream is = getClass().getResourceAsStream(location);
        if (is == null) {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(location);
        }
        return is;
    }

    private JmxMetricMetadata metadataOf(Map.Entry<String, List<MetricProperty>> metadataEntry) {
        String name = metadataEntry.getKey();
        Map<String, String> entryProperties = new HashMap<>();
        metadataEntry.getValue()
                .forEach(
                        prop -> entryProperties.put(prop.propertyKey, prop.propertyValue));
        List<String> tagsToFill = new ArrayList<>();
        if (entryProperties.containsKey("tagsToFill")) {
            tagsToFill = Arrays.asList(entryProperties.get("tagsToFill").split(","));
        }

        final MeasurementUnit unit = (entryProperties.get("unit") == null) ? NONE : MeasurementUnit.valueOf(entryProperties.get("unit").toUpperCase());

        JmxMetricMetadata metadata = new JmxMetricMetadata(name,
                entryProperties.get("description"),
                unit,
                MetricMetadata.Type.valueOf(entryProperties.get("type").toUpperCase()),
                entryProperties.get("mbean"),
                tagsToFill,
                Collections.emptyList());

        return metadata;
    }

    private static class MetricProperty {
        MetricProperty(Map.Entry<Object, Object> keyValue) {
            String key = (String) keyValue.getKey();
            int propertyIdEnd = key.lastIndexOf('.');
            metricName = key.substring(0, propertyIdEnd);
            propertyKey = key.substring(propertyIdEnd + 1);
            propertyValue = (String) keyValue.getValue();
        }

        String metricName;
        String propertyKey;
        String propertyValue;

        String getMetricName() {
            return metricName;
        }
    }

    private static OptionalDouble getValueFromMBean(MBeanServer mbs, String mbeanExpression) {
        checkNotNullParam("mbs", mbs);
        checkNotNullParam("mbeanExpression", mbeanExpression);

        if (!mbeanExpression.contains("/")) {
            throw new IllegalArgumentException(mbeanExpression);
        }

        int slashIndex = mbeanExpression.indexOf('/');
        String mbean = mbeanExpression.substring(0, slashIndex);
        String attName = mbeanExpression.substring(slashIndex + 1);
        String subItem = null;
        if (attName.contains("#")) {
            int hashIndex = attName.indexOf('#');
            subItem = attName.substring(hashIndex + 1);
            attName = attName.substring(0, hashIndex);
        }

        try {
            ObjectName objectName = new ObjectName(mbean);
            Object attribute = mbs.getAttribute(objectName, attName);
            if (attribute instanceof Number) {
                Number num = (Number) attribute;
                return OptionalDouble.of(num.doubleValue());
            } else if (attribute instanceof CompositeData) {
                CompositeData compositeData = (CompositeData) attribute;
                Number num = (Number) compositeData.get(subItem);
                return OptionalDouble.of(num.doubleValue());
            } else {
                return OptionalDouble.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
