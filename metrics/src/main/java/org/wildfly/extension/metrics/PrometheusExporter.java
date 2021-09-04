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
package org.wildfly.extension.metrics;

import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.wildfly.extension.metrics.MetricMetadata.MetricTag;

public class PrometheusExporter {

    private static final String LF = "\n";

    public String export(WildFlyMetricRegistry registry) {
        Set<String> alreadyExportedMetrics = new HashSet<String>();

        StringBuilder out = new StringBuilder();

        for (Map.Entry<MetricID, Metric> entry : registry.getMetrics().entrySet()) {
            MetricID metricID = entry.getKey();
            String metricName = metricID.getMetricName();
            MetricMetadata metadata = registry.getMetricMetadata().get(metricName);
            String prometheusMetricName = toPrometheusMetricName(metricID, metadata);
            OptionalDouble metricValue = entry.getValue().getValue();
            // if the metric does not return a value, we skip printing the HELP and TYPE
            if (!metricValue.isPresent()) {
                continue;
            }
            if (!alreadyExportedMetrics.contains(metricName)) {
                out.append("# HELP " + prometheusMetricName + " " + metadata.getDescription());
                out.append(LF);
                out.append("# TYPE " + prometheusMetricName + " " + metadata.getType());
                out.append(LF);
                alreadyExportedMetrics.add(metricName);
            }
            double scaledValue = scaleToBaseUnit(metricValue.getAsDouble(), metadata.getMeasurementUnit());
            // I'm pretty sure this is incorrect but that aligns with smallrye-metrics OpenMetricsExporter behaviour
            if (metadata.getType() == MetricMetadata.Type.COUNTER && metadata.getMeasurementUnit() != MeasurementUnit.NONE) {
                prometheusMetricName += "_" + metadata.getBaseMetricUnit();
            }
            out.append(prometheusMetricName + getTagsAsAString(metricID) + " " + scaledValue);
            out.append(LF);
        }

        return out.toString();
    }

    private static double scaleToBaseUnit(double value, MeasurementUnit unit) {
        return value * MeasurementUnit.calculateOffset(unit, unit.getBaseUnits());
    }

    private static String toPrometheusMetricName(MetricID metricID, MetricMetadata metadata) {
        String prometheusName = metricID.getMetricName();
        // change the Prometheus name depending on type and measurement unit
        if (metadata.getType() == WildFlyMetricMetadata.Type.COUNTER) {
            prometheusName += "_total";
        } else {
            // if it's a gauge, let's add the base unit to the prometheus name
            String baseUnit = metadata.getBaseMetricUnit();
            if (!MetricMetadata.NONE.equals(baseUnit)) {
                prometheusName += "_" + baseUnit;
            }
        }
        return prometheusName;
    }

    public static String getTagsAsAString(MetricID metricID) {
        MetricTag[] tags = metricID.getTags();
        if (tags.length == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder("{");
        for (int i = 0; i < tags.length; i++) {
            if (i > 0) {
                out.append(",");
            }
            MetricTag tag = tags[i];
            out.append(tag.getKey() + "=\"" + tag.getValue() + "\"");
        }
        return out.append("}").toString();
    }
}