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
package org.wildfly.extension.metrics.api;

import static java.util.Objects.requireNonNull;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.metrics.internal.PrometheusExporter.getPrometheusMetricName;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.MeasurementUnit;

public class WildFlyMetricMetadata implements MetricMetadata{

    private final String description;
    private final MeasurementUnit unit;
    private final MetricMetadata.Type type;
    private final String attributeName;
    private final PathAddress address;
    private final String globalPrefix;
    private String metricName;
    private MetricMetadata.MetricTag[] tags;
    private MetricID metricID;

    public WildFlyMetricMetadata(String attributeName, PathAddress address, String globalPrefix, String description, MeasurementUnit unit, MetricMetadata.Type type) {
        requireNonNull(attributeName);
        requireNonNull(address);
        requireNonNull(globalPrefix);
        requireNonNull(description);
        requireNonNull(type);

        this.attributeName = attributeName;
        this.address = address;
        this.globalPrefix = globalPrefix;
        this.description = description;
        this.unit = unit != null ? unit : MeasurementUnit.NONE;
        this.type = type;

        init();
    }

    private void init() {
        String metricPrefix = "";
        List<String> labelNames = new ArrayList<>();
        List<String> labelValues = new ArrayList<>();
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
        tags = new MetricMetadata.MetricTag[labelNames.size()];
        for (int i = 0; i < labelNames.size(); i++) {
            String name = labelNames.get(i);
            String value = labelValues.get(i);
            tags[i] = new MetricMetadata.MetricTag(name, value);
        }
        metricID = new MetricID(metricName, tags);
    }

    public String getMetricName() {
        return metricName;
    }

    public MetricMetadata.MetricTag[] getTags() {
        return tags;
    }

    public String getDescription() {
        return description;
    }

    public MeasurementUnit getMeasurementUnit() {
        return unit;
    }

    public MetricMetadata.Type getType() {
        return type;
    }

    public MetricID getMetricID() {
        return metricID;
    }


}
