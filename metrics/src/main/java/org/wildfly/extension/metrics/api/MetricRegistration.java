package org.wildfly.extension.metrics.api;

import java.util.ArrayList;
import java.util.List;

public abstract class MetricRegistration {

    private final List<Runnable> registrationTasks = new ArrayList<>();
    protected final List<MetricID> unregistrationTasks = new ArrayList<>();

    public synchronized void register() { // synchronized to avoid registering same thing twice. Shouldn't really be possible; just being cautious
        for (Runnable task : registrationTasks) {
            task.run();
        }
        // This object will last until undeploy or server stop,
        // so clean up and save memory
        registrationTasks.clear();
    }

    void addRegistrationTask(Runnable task) {
        registrationTasks.add(task);
    }

    void addUnregistrationTask(MetricID metricID) {
        unregistrationTasks.add(metricID);
    }

    public abstract void registerMetric(WildFlyMetricMetadata metadata, WildFlyMetric metric);

    public abstract void unregister();
}
