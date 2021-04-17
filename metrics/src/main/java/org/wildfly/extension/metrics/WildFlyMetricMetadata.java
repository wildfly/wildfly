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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.common.Assert.checkNotNullParamWithNullPointerException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.MeasurementUnit;

public class WildFlyMetricMetadata implements MetricMetadata {

    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("(?<=[a-z])[A-Z]");

    private final String description;
    private final MeasurementUnit unit;
    private final Type type;
    private final String attributeName;
    private final PathAddress address;
    private final String globalPrefix;
    private String metricName;
    private MetricTag[] tags;
    private MetricID metricID;

    public WildFlyMetricMetadata(String attributeName, PathAddress address, String prefix, String description, MeasurementUnit unit, Type type) {
        this.attributeName = checkNotNullParamWithNullPointerException("attributeName", attributeName);
        this.address = checkNotNullParamWithNullPointerException("address", address);
        this.description = checkNotNullParamWithNullPointerException("description", description);
        this.type = checkNotNullParamWithNullPointerException("type", type);
        this.globalPrefix = prefix;

        this.unit = unit != null ? unit : MeasurementUnit.NONE;

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
        tags = new MetricTag[labelNames.size()];
        for (int i = 0; i < labelNames.size(); i++) {
            String name = labelNames.get(i);
            String value = labelValues.get(i);
            tags[i] = new MetricTag(name, value);
        }
        metricID = new MetricID(metricName, tags);
    }

    @Override
    public String getMetricName() {
        return metricName;
    }

    @Override
    public MetricTag[] getTags() {
        return tags;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public MeasurementUnit getMeasurementUnit() {
        return unit;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public MetricID getMetricID() {
        return metricID;
    }

    static String getPrometheusMetricName(String name) {
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
