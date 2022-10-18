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
    private List<String> tagsToFill;
    private List<MetricTag> tags;

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
