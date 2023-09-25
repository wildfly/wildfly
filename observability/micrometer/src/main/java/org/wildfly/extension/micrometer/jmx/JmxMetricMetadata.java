/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer.jmx;

import java.util.List;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.wildfly.extension.micrometer.metrics.MetricID;
import org.wildfly.extension.micrometer.metrics.MetricMetadata;

class JmxMetricMetadata implements MetricMetadata {
    private final String name;
    private final String description;
    private final MeasurementUnit unit;
    private final Type type;
    private final String mbean;
    private final MetricID metricID;
    private final List<String> tagsToFill;
    private final List<MetricTag> tags;

    JmxMetricMetadata(String name, String description, MeasurementUnit unit, Type type, String mbean, List<String> tagsToFill, List<MetricTag> tags) {
        this.name = name;
        this.description = description;
        this.unit = unit;
        this.type = type;
        this.mbean = mbean;
        this.tagsToFill = tagsToFill;
        this.tags = tags;
        this.metricID = new MetricID(name, tags.toArray(new MetricTag[0]));
    }

    String getMBean() {
        return mbean;
    }

    List<String> getTagsToFill() {
        return tagsToFill;
    }

    @Override
    public MetricID getMetricID() {
        return metricID;
    }

    @Override
    public String getMetricName() {
        return name;
    }

    @Override
    public MetricTag[] getTags() {
        return tags.toArray(new MetricTag[0]);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public MeasurementUnit getMeasurementUnit() {
        return unit;
    }
}
