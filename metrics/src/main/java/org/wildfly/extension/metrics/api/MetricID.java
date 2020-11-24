package org.wildfly.extension.metrics.api;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class MetricID implements Comparable<MetricID>{
    private final String metricName;
    private final Map<String, String> tags = new TreeMap<>();

    public MetricID(String metricName, WildFlyMetricMetadata.MetricTag[] metricTags) {
        this.metricName = metricName;
        for (int i = 0; i < metricTags.length; i++) {
            this.tags.put(metricTags[i].getKey(), metricTags[i].getValue());
        }
    }

    public String getName() {
        return metricName;
    }

    public Map<String, String> getTags() {
        return tags;
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

    public String getTagsAsAString() {
        if (tags.isEmpty()) {
            return "";
        }
        String tagsAsString = tags.entrySet().stream().map(e -> e.getKey() + "=\"" + e.getValue() + "\"").collect(Collectors.joining(","));
        return "{" + tagsAsString + "}";
    }
}