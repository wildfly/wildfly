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

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class MetricID implements Comparable<MetricID> {
    private String metricName;
    // keep a map of tags to ensure that the identity of the metrics does not differ if the
    // tags are not in the same order in the array
    private final Map<String, String> tags = new TreeMap<>();

    public MetricID(String metricName, WildFlyMetricMetadata.MetricTag[] tags) {
        this.metricName = metricName;
        for (WildFlyMetricMetadata.MetricTag tag : tags) {
            this.tags.put(tag.getKey(), tag.getValue());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
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
