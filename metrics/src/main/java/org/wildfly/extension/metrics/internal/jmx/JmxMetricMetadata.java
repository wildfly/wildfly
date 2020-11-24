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

import java.util.List;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.wildfly.extension.metrics.api.MetricID;
import org.wildfly.extension.metrics.api.MetricMetadata;

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
