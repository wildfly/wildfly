/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.metrics.internal.jmx;

import static org.jboss.as.controller.client.helpers.MeasurementUnit.NONE;

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
import org.wildfly.extension.metrics.internal.Metric;
import org.wildfly.extension.metrics.internal.MetricCollector;
import org.wildfly.extension.metrics.internal.WildFlyMetricMetadata;
import org.wildfly.extension.metrics.internal.WildFlyMetricRegistry;

public class JmxMetricCollector {
    private final MBeanServer mbs;
    private WildFlyMetricRegistry registry;

    public JmxMetricCollector(WildFlyMetricRegistry registry) {
        this.registry = registry;
        this.mbs = ManagementFactory.getPlatformMBeanServer();
    }

    public void init() throws IOException {
        register("jmx-metrics.properties", registry);
    }


    private void register(String propertiesFile, WildFlyMetricRegistry registry) throws IOException {
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
                    List<MetricCollector.MetricTag> tags = new ArrayList<>(metadata.getTagsToFill().size());
                    for (String key : metadata.getTagsToFill()) {
                        String value = objectName.getKeyProperty(key);
                        tags.add(new MetricCollector.MetricTag(key, value));
                    }
                    expandedConfigs.add(new JmxMetricMetadata(metadata.getName(),
                            metadata.getDescription(),
                            metadata.getUnit(),
                            metadata.getType(),
                            objectName.getCanonicalName() + "/" + attribute,
                            Collections.emptyList(),
                            tags));

                }
                // now, it has been expanded, remove the "multi" mbean
                iterator.remove();
            } catch (MalformedObjectNameException e) {
                e.printStackTrace();
            }
        }
        configs.addAll(expandedConfigs);

        for (JmxMetricMetadata config : configs) {
            register(registry, config, config.getTags());
        }
    }

    void register(WildFlyMetricRegistry registry, JmxMetricMetadata config, List<MetricCollector.MetricTag> tags) {
        Metric metric = new Metric() {
            @Override
            public OptionalDouble getValue() {
                try {
                    return getValueFromMBean(mbs, config.getMBean());
                } catch (Exception e) {
                    return OptionalDouble.empty();
                }
            }
        };
        registry.register(config, metric, tags.toArray(new MetricCollector.MetricTag[0]));
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
                .sorted(Comparator.comparing(e -> e.getName()))
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
                WildFlyMetricMetadata.Type.valueOf(entryProperties.get("type").toUpperCase()),
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

        if (mbeanExpression == null) {
            throw new IllegalArgumentException("MBean Expression is null");
        }
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
