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

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class MetricID implements Comparable<MetricID> {
    private final String metricName;
    private final WildFlyMetricMetadata.MetricTag[] metricTags;
    // keep a map of tags to ensure that the identity of the metrics does not differ if the
    // tags are not in the same order in the array
    private final Map<String, String> tags = new TreeMap<>();

    public MetricID(String metricName, WildFlyMetricMetadata.MetricTag[] tags) {
        this.metricName = metricName;
        this.metricTags = tags;
        for (WildFlyMetricMetadata.MetricTag tag : tags) {
            this.tags.put(tag.getKey(), tag.getValue());
        }
    }

    public String getMetricName() {
        return metricName;
    }
    public WildFlyMetricMetadata.MetricTag[] getTags() {
        return metricTags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetricID metricID = (MetricID) o;
        return metricName.equals(metricID.metricName) &&
                tags.equals(metricID.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metricName, tags);
    }

    @Override
    public int compareTo(MetricID other) {
        int compareVal = this.metricName.compareTo(other.metricName);
        if (compareVal == 0) {
            compareVal = this.tags.size() - other.tags.size();
            if (compareVal == 0) {
                Iterator<Map.Entry<String, String>> thisIterator = tags.entrySet().iterator();
                Iterator<Map.Entry<String, String>> otherIterator = other.tags.entrySet().iterator();
                while (thisIterator.hasNext() && otherIterator.hasNext()) {
                    Map.Entry<String, String> thisEntry = thisIterator.next();
                    Map.Entry<String, String> otherEntry = otherIterator.next();
                    compareVal = thisEntry.getKey().compareTo(otherEntry.getKey());
                    if (compareVal != 0) {
                        return compareVal;
                    } else {
                        compareVal = thisEntry.getValue().compareTo(otherEntry.getValue());
                        if (compareVal != 0) {
                            return compareVal;
                        }
                    }
                }
            }
        }
        return compareVal;
    }
}