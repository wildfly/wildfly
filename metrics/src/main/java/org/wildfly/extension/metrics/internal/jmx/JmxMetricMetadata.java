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
import org.wildfly.extension.metrics.internal.MetricCollector;
import org.wildfly.extension.metrics.internal.WildFlyMetricMetadata;

class JmxMetricMetadata extends WildFlyMetricMetadata {
    private final String mbean;
    private List<String> tagsToFill;
    private List<MetricCollector.MetricTag> tags;

    JmxMetricMetadata(String name, String description, MeasurementUnit unit, Type type, String mbean, List<String> tagsToFill, List<MetricCollector.MetricTag> tags) {
        super(name, description, unit, type);
        this.mbean = mbean;
        this.tagsToFill = tagsToFill;
        this.tags = tags;
    }

    String getMBean() {
        return mbean;
    }

    List<MetricCollector.MetricTag> getTags() {
        return tags;
    }

    List<String> getTagsToFill() {
        return tagsToFill;
    }
}
