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

package org.wildfly.extension.micrometer.metrics;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.common.Assert.checkNotNullParamWithNullPointerException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.MeasurementUnit;

public class WildFlyMetricMetadata implements MetricMetadata {
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("(?<=[a-z])[A-Z]");
    private static final String STATISTICS = "statistics";

    private final String description;
    private final MeasurementUnit unit;
    private final Type type;
    private final String attributeName;
    private final PathAddress address;
    private String metricName;
    private MetricTag[] tags;
    private MetricID metricID;

    public WildFlyMetricMetadata(String attributeName,
                                 PathAddress address,
                                 String description,
                                 MeasurementUnit unit,
                                 Type type) {
        this.attributeName = checkNotNullParamWithNullPointerException("attributeName", attributeName);
        this.address = checkNotNullParamWithNullPointerException("address", address);
        this.description = checkNotNullParamWithNullPointerException("description", description);
        this.type = checkNotNullParamWithNullPointerException("type", type);

        this.unit = unit != null ? unit : MeasurementUnit.NONE;

        init();
    }

    private void init() {
        StringBuilder metricPrefix = new StringBuilder();
        List<String> labelNames = new ArrayList<>();
        List<String> labelValues = new ArrayList<>();
        for (PathElement element : address) {
            String key = element.getKey();
            String value = element.getValue();
            // prepend the subsystem or statistics name to the attribute
            if (key.equals(SUBSYSTEM) || key.equals(STATISTICS)) {
                metricPrefix.append(value)
                        .append("-");
                continue;
            }
            if (!key.equals(DEPLOYMENT) && !key.equals(SUBDEPLOYMENT)) {
                labelNames.add("type");
                labelValues.add(key);

                labelNames.add("name");
                labelValues.add(value);
            } else {
                labelNames.add(getDottedName(key));
                labelValues.add(value);
            }
        }
        // if the resource address defines a deployment (without subdeployment),
        int deploymentIndex = labelNames.indexOf(DEPLOYMENT);
        int subdeploymentIndex = labelNames.indexOf(SUBDEPLOYMENT);
        if (deploymentIndex > -1 && subdeploymentIndex == -1) {
            labelNames.add(SUBDEPLOYMENT);
            subdeploymentIndex = labelNames.size()-1;
            labelValues.add(labelValues.get(deploymentIndex));
        }
        if (deploymentIndex == -1) {
            labelNames.add(DEPLOYMENT);
            labelValues.add("");
        }
        if (subdeploymentIndex == -1) {
            labelNames.add(SUBDEPLOYMENT);
            labelValues.add("");
        }

        if (!labelNames.contains("app")) {
            labelNames.add("app");
            labelValues.add(deploymentIndex >= 0 ? labelValues.get(deploymentIndex) : "wildfly");
        }

        metricName = getDottedName(metricPrefix + attributeName);
        tags = new MetricTag[labelNames.size()];
        for (int i = 0; i < labelNames.size(); i++) {
            tags[i] = new MetricTag(labelNames.get(i), labelValues.get(i));
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

    private static String getDottedName(String name) {
        return decamelize(name.replaceAll("[^\\w]+", "."));
    }


    private static String decamelize(String in) {
        Matcher m = SNAKE_CASE_PATTERN.matcher(in);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "." + m.group().toLowerCase());
        }
        m.appendTail(sb);
        return sb.toString().toLowerCase();
    }

    @Override
    public String toString() {
        return ("WildFlyMetricMetadata{" +
                "description='" + description + '\'' +
                ", unit=" + unit +
                ", type=" + type +
                ", attributeName='" + attributeName + '\'' +
                ", address=" + address +
                ", metricName='" + metricName + '\'' +
                ", tags=" + Arrays.toString(tags) +
                ", metricID=" + metricID +
                '}').replaceAll("\\n", "");
    }
}
