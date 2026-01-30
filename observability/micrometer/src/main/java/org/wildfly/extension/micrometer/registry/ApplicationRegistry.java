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
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
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

    private Meter.Id addMissingTagToId(Meter.Id id) {
        if (id.getTag(TAG_WF_DEPLOYMENT) != null) {
            return id;
        }
        return id.withTag(Tag.of(TAG_WF_DEPLOYMENT, deploymentName));
    }

    // Override protected newXxx() methods to forward Builder pattern registrations to parentRegistry

    @Override
    protected <T> Gauge newGauge(Meter.Id id, T obj, ToDoubleFunction<T> valueFunction) {
        Meter.Id taggedId = addMissingTagToId(id);
        Gauge.Builder<T> builder = Gauge.builder(taggedId.getName(), obj, valueFunction)
                .tags(taggedId.getTags());
        if (taggedId.getDescription() != null) {
            builder.description(taggedId.getDescription());
        }
        if (taggedId.getBaseUnit() != null) {
            builder.baseUnit(taggedId.getBaseUnit());
        }
        return builder.register(parentRegistry);
    }

    @Override
    protected Counter newCounter(Meter.Id id) {
        Meter.Id taggedId = addMissingTagToId(id);
        Counter.Builder builder = Counter.builder(taggedId.getName())
                .tags(taggedId.getTags());
        if (taggedId.getDescription() != null) {
            builder.description(taggedId.getDescription());
        }
        if (taggedId.getBaseUnit() != null) {
            builder.baseUnit(taggedId.getBaseUnit());
        }
        return builder.register(parentRegistry);
    }

    @Override
    protected Timer newTimer(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig,
                             PauseDetector pauseDetector) {
        Meter.Id taggedId = addMissingTagToId(id);
        Timer.Builder builder = Timer.builder(taggedId.getName())
                .tags(taggedId.getTags());
        if (taggedId.getDescription() != null) {
            builder.description(taggedId.getDescription());
        }
        return builder.register(parentRegistry);
    }

    @Override
    protected DistributionSummary newDistributionSummary(Meter.Id id,
                                                          DistributionStatisticConfig distributionStatisticConfig,
                                                          double scale) {
        Meter.Id taggedId = addMissingTagToId(id);
        DistributionSummary.Builder builder = DistributionSummary.builder(taggedId.getName())
                .tags(taggedId.getTags())
                .scale(scale);
        if (taggedId.getDescription() != null) {
            builder.description(taggedId.getDescription());
        }
        if (taggedId.getBaseUnit() != null) {
            builder.baseUnit(taggedId.getBaseUnit());
        }
        return builder.register(parentRegistry);
    }

    @Override
    protected <T> FunctionTimer newFunctionTimer(Meter.Id id, T obj, ToLongFunction<T> countFunction,
                                                  ToDoubleFunction<T> totalTimeFunction, TimeUnit totalTimeFunctionUnit) {
        Meter.Id taggedId = addMissingTagToId(id);
        FunctionTimer.Builder<T> builder = FunctionTimer.builder(taggedId.getName(), obj,
                        countFunction, totalTimeFunction, totalTimeFunctionUnit)
                .tags(taggedId.getTags());
        if (taggedId.getDescription() != null) {
            builder.description(taggedId.getDescription());
        }
        return builder.register(parentRegistry);
    }

    @Override
    protected <T> FunctionCounter newFunctionCounter(Meter.Id id, T obj, ToDoubleFunction<T> countFunction) {
        Meter.Id taggedId = addMissingTagToId(id);
        FunctionCounter.Builder<T> builder = FunctionCounter.builder(taggedId.getName(), obj, countFunction)
                .tags(taggedId.getTags());
        if (taggedId.getDescription() != null) {
            builder.description(taggedId.getDescription());
        }
        if (taggedId.getBaseUnit() != null) {
            builder.baseUnit(taggedId.getBaseUnit());
        }
        return builder.register(parentRegistry);
    }

    @Override
    protected Meter newMeter(Meter.Id id, Meter.Type type, Iterable<Measurement> measurements) {
        Meter.Id taggedId = addMissingTagToId(id);
        return Meter.builder(taggedId.getName(), type, measurements)
                .tags(taggedId.getTags())
                .description(taggedId.getDescription())
                .baseUnit(taggedId.getBaseUnit())
                .register(parentRegistry);
    }

    @Override
    protected LongTaskTimer newLongTaskTimer(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig) {
        Meter.Id taggedId = addMissingTagToId(id);
        LongTaskTimer.Builder builder = LongTaskTimer.builder(taggedId.getName())
                .tags(taggedId.getTags());
        if (taggedId.getDescription() != null) {
            builder.description(taggedId.getDescription());
        }
        return builder.register(parentRegistry);
    }

    @Override
    protected <T> TimeGauge newTimeGauge(Meter.Id id, T obj, TimeUnit valueFunctionUnit,
                                          ToDoubleFunction<T> valueFunction) {
        Meter.Id taggedId = addMissingTagToId(id);
        TimeGauge.Builder<T> builder = TimeGauge.builder(taggedId.getName(), obj, valueFunctionUnit, valueFunction)
                .tags(taggedId.getTags());
        if (taggedId.getDescription() != null) {
            builder.description(taggedId.getDescription());
        }
        return builder.register(parentRegistry);
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
