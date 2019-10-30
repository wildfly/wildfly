package org.wildfly.extension.microprofile.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;

public class PrometheusCollector extends Collector implements Collector.Describable {

    // mapping between the metric name and the MetricFamilySamples that provides its values
    private Map<String, MetricFamilySamples> metricNames = new TreeMap<>();
    // each MetricFamilySamples has list of Sample's supplier (that can be optional) if the underlying metric value is undefined.
    private Map<String, List<Supplier<Optional<Sample>>>> metricFamilyMap = new HashMap();

    public synchronized void addMetricFamilySamples(MetricFamilySamples mfs) {
        if (!metricNames.containsKey(mfs.name)) {
            metricNames.put(mfs.name, mfs);
            metricFamilyMap.put(mfs.name, new CopyOnWriteArrayList<>());
        }
    }

    void addMetricFamilySampleSupplier(MetricFamilySamples mfs, Supplier<Optional<Sample>> sampleSupplier) {
        addMetricFamilySamples(mfs);
        metricFamilyMap.get(mfs.name).add(sampleSupplier);
    }

    void removeMetricFamilySampleSupplier(String metricName, Supplier<Optional<Sample>> sampleSupplier) {
        List<Supplier<Optional<Sample>>> suppliers = metricFamilyMap.get(metricName);
        if (suppliers != null) {
            suppliers.remove(sampleSupplier);
        }
    }


    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> samples = new ArrayList<>();
        for (Map.Entry<String, List<Supplier<Optional<Sample>>>> entry : metricFamilyMap.entrySet()) {
            String metricName = entry.getKey();
            MetricFamilySamples mfs = metricNames.get(metricName);
            mfs.samples.clear();
            for (Supplier<Optional<Sample>> sampleSupplier : entry.getValue()) {
                Optional<Sample> sample = sampleSupplier.get();
                if (sample.isPresent()) {
                    mfs.samples.add(sample.get());
                }
            }
            if (!mfs.samples.isEmpty()) {
                samples.add(mfs);
            }
        }
        return samples;
    }

    @Override
    public List<MetricFamilySamples> describe() {
        return new ArrayList<>(metricNames.values());
    }

}
