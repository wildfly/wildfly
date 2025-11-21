/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer.registry;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import java.util.stream.StreamSupport;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.RequiredSearch;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.wildfly.extension.micrometer.MicrometerExtensionLogger;

public class ApplicationRegistry extends SimpleMeterRegistry {
    static final String TAG_WF_DEPLOYMENT = "WF_DEPLOYMENT";
    private final String deploymentName;
    private final MeterRegistry parentRegistry;
    private final More more = new ApplicationMore();

    public ApplicationRegistry(String deploymentName, MeterRegistry parentRegistry) {
        this.deploymentName = deploymentName;
        this.parentRegistry = parentRegistry;
    }

    @Override
    public List<Meter> getMeters() {
        return parentRegistry.getMeters().stream().filter(m -> !containsWrongDeploymentTag(m.getId()))
                .toList();
    }

    @Override
    public void forEachMeter(Consumer<? super Meter> consumer) {
        parentRegistry.forEachMeter(m -> {
            if (!containsWrongDeploymentTag(m.getId())) {
                consumer.accept(m);
            }
        });
    }

    @Override
    public Search find(String name) {
        return parentRegistry.find(name).tag(TAG_WF_DEPLOYMENT, deploymentName);
    }

    @Override
    public RequiredSearch get(String name) {
        return parentRegistry.get(name).tag(TAG_WF_DEPLOYMENT, deploymentName);
    }

    @Override
    public Meter remove(Meter meter) {
        if (containsWrongDeploymentTag(meter.getId())) {
            return null;
        }
        return parentRegistry.remove(meter.getId());
    }

    @Override
    public Meter removeByPreFilterId(Meter.Id id) {
        if (containsWrongDeploymentTag(id)) {
            return null;
        }
        return parentRegistry.removeByPreFilterId(id);
    }

    @Override
    public Meter remove(Meter.Id id) {
        if (containsWrongDeploymentTag(id)) {
            return null;
        }
        return parentRegistry.remove(id);
    }

    @Override
    public Counter counter(String name, Iterable<Tag> tags) {
        var counter = findByNameAndTags(name, tags).counter();

        if (counter == null || containsWrongDeploymentTag(counter.getId()) ) {

            counter = parentRegistry.counter(name, addMissingTag(tags));
        }
        return counter;
    }

    @Override
    public <T> T gauge(String name, Iterable<Tag> tags, T stateObject, ToDoubleFunction<T> valueFunction) {
        var gauge = findByNameAndTags(name, tags).gauge();

        if (gauge == null || containsWrongDeploymentTag(gauge.getId()) ) {
            parentRegistry.gauge(name, addMissingTag(tags), stateObject, valueFunction);
        }

        return stateObject;
    }

    @Override
    public Timer timer(String name, Iterable<Tag> tags) {
        var timer = findByNameAndTags(name, tags).timer();
        if (timer == null || containsWrongDeploymentTag(timer.getId()) ) {
            timer = parentRegistry.timer(name, addMissingTag(tags));
        }
        return timer;
    }

    @Override
    public DistributionSummary summary(String name, Iterable<Tag> tags) {
        var summary = findByNameAndTags(name, tags).summary();
        if (summary == null || containsWrongDeploymentTag(summary.getId()) ) {
            summary = parentRegistry.summary(name, addMissingTag(tags));
        }
        return summary;
    }

    @Override
    public void clear() {
        getMeters().forEach(this::remove);
    }

    @Override
    public Config config() {
        MicrometerExtensionLogger.MICROMETER_LOGGER.configNotSupported();
        return super.config();
    }

    @Override
    public More more() {
        return more;
    }

    private Search findByNameAndTags(String name, Iterable<Tag> tags) {
        return parentRegistry.find(name).tags(tags);
    }

    private Iterable<Tag> addMissingTag(Iterable<Tag> tags) {
        var hasWfTag =  StreamSupport.stream(tags.spliterator(), false)
                .anyMatch(t -> t.getKey().equals(TAG_WF_DEPLOYMENT));
        return hasWfTag ? tags : Tags.concat(tags, TAG_WF_DEPLOYMENT, deploymentName);
    }

    private boolean containsWrongDeploymentTag(Meter.Id id) {
        // If the tag doesn't match the current deployment, whether it's missing or different, it should be denied
        return !deploymentName.equals(id.getTag(TAG_WF_DEPLOYMENT));
    }

    private class ApplicationMore extends More {
        @Override
        public <T> FunctionCounter counter(String name, Iterable<Tag> tags, T obj, ToDoubleFunction<T> countFunction) {
            var meter = findByNameAndTags(name, tags).functionCounter();
            if (meter == null || containsWrongDeploymentTag(meter.getId()) ) {
                meter = parentRegistry.more().counter(name, addMissingTag(tags), obj, countFunction);
            }
            return meter;
        }

        @Override
        public <T extends Number> FunctionCounter counter(String name, Iterable<Tag> tags, T number) {
            var meter = findByNameAndTags(name, tags).functionCounter();
            if (meter == null || containsWrongDeploymentTag(meter.getId()) ) {
                meter = parentRegistry.more().counter(name, addMissingTag(tags), number);
            }
            return meter;
        }

        @Override
        public LongTaskTimer longTaskTimer(String name, Iterable<Tag> tags) {
            var meter = findByNameAndTags(name, tags).longTaskTimer();
            if (meter == null || containsWrongDeploymentTag(meter.getId()) ) {
                meter = parentRegistry.more().longTaskTimer(name, addMissingTag(tags));
            }
            return meter;
        }

        @Override
        public <T> TimeGauge timeGauge(String name, Iterable<Tag> tags, T obj, TimeUnit timeFunctionUnit,
                                       ToDoubleFunction<T> timeFunction) {
            var meter = findByNameAndTags(name, tags).timeGauge();
            if (meter == null || containsWrongDeploymentTag(meter.getId()) ) {
                meter = parentRegistry.more().timeGauge(name, addMissingTag(tags), obj, timeFunctionUnit, timeFunction);
            }
            return meter;
        }

        @Override
        public <T> FunctionTimer timer(String name, Iterable<Tag> tags, T obj, ToLongFunction<T> countFunction,
                                       ToDoubleFunction<T> totalTimeFunction, TimeUnit totalTimeFunctionUnit) {
            var meter = findByNameAndTags(name, tags).functionTimer();
            if (meter == null || containsWrongDeploymentTag(meter.getId()) ) {
                meter = parentRegistry.more().timer(name, addMissingTag(tags), obj, countFunction, totalTimeFunction, totalTimeFunctionUnit);
            }
            return meter;
        }
    }
}
