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
package org.wildfly.extension.metrics.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.as.controller.client.helpers.MeasurementUnit;

public class PrometheusExporter {

    private static final String LF = "\n";
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("(?<=[a-z])[A-Z]");

    public static String getPrometheusMetricName(String name) {
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

    public String export(WildFlyMetricRegistry registry) {
        Set<String> alreadyExportedMetrics = new HashSet<String>();

        StringBuilder out = new StringBuilder();

        for (Map.Entry<WildFlyMetricRegistry.MetricID, Metric> entry : registry.getMetrics().entrySet()) {
            WildFlyMetricRegistry.MetricID metricID = entry.getKey();
            String metricName = metricID.getName();
            WildFlyMetricMetadata metadata = registry.getMetricMetadata().get(metricName);
            String prometheusMetricName = toPrometheusMetricName(metricID, metadata);
            OptionalDouble metricValue = entry.getValue().getValue();
            // if the metric does not return a value, we skip printing the HELP and TYPE
            if (!metricValue.isPresent()) {
                break;
            }
            if (!alreadyExportedMetrics.contains(metricName)) {
                out.append("# HELP " + prometheusMetricName + " " + metadata.getDescription());
                out.append(LF);
                out.append("# TYPE " + prometheusMetricName + " " + metadata.getType());
                out.append(LF);
                alreadyExportedMetrics.add(metricName);
            }
            double scaledValue = scaleToBaseUnit(metricValue.getAsDouble(), metadata.getUnit());
            out.append(prometheusMetricName + metricID.getTagsAsAString() + " " + scaledValue);
            out.append(LF);
        }

        return out.toString();
    }

    private static double scaleToBaseUnit(double value, MeasurementUnit unit) {
        return value * MeasurementUnit.calculateOffset(unit, unit.getBaseUnits());
    }

    private String toPrometheusMetricName(WildFlyMetricRegistry.MetricID metricID, WildFlyMetricMetadata metadata) {
        String prometheusName = metricID.getName();
        // change the Prometheus name depending on type and measurement unit
        if (metadata.getType() == WildFlyMetricMetadata.Type.COUNTER) {
            prometheusName += "_total";
        } else {
            // if it's a gauge, let's add the base unit to the prometheus name
            String baseUnit = metadata.getBaseMetricUnit();
            if (baseUnit != "none") {
                prometheusName += "_" + baseUnit;
            }
        }
        return prometheusName;
    }
}
