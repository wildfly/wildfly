/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer.registry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.core.instrument.noop.NoopCounter;
import io.micrometer.core.instrument.noop.NoopDistributionSummary;
import io.micrometer.core.instrument.noop.NoopFunctionCounter;
import io.micrometer.core.instrument.noop.NoopFunctionTimer;
import io.micrometer.core.instrument.noop.NoopGauge;
import io.micrometer.core.instrument.noop.NoopMeter;
import io.micrometer.core.instrument.noop.NoopTimer;

public class NoOpRegistry extends MeterRegistry implements WildFlyRegistry {

    public NoOpRegistry() {
        super(Clock.SYSTEM);
    }


    @Override
    protected <T> Gauge newGauge(Meter.Id id, T t, ToDoubleFunction<T> toDoubleFunction) {
        return new NoopGauge(id);
    }

    @Override
    protected Counter newCounter(Meter.Id id) {
        return new NoopCounter(id);
    }

    @Override
    protected Timer newTimer(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig, PauseDetector pauseDetector) {
        return new NoopTimer(id);
    }

    @Override
    protected DistributionSummary newDistributionSummary(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig, double v) {
        return new NoopDistributionSummary(id);
    }

    @Override
    protected Meter newMeter(Meter.Id id, Meter.Type type, Iterable<Measurement> iterable) {
        return new NoopMeter(id);
    }

    @Override
    protected <T> FunctionTimer newFunctionTimer(Meter.Id id, T t, ToLongFunction<T> toLongFunction, ToDoubleFunction<T> toDoubleFunction, TimeUnit timeUnit) {
        return new NoopFunctionTimer(id);
    }

    @Override
    protected <T> FunctionCounter newFunctionCounter(Meter.Id id, T t, ToDoubleFunction<T> toDoubleFunction) {
        return new NoopFunctionCounter(id);
    }

    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.SECONDS;
    }

    @Override
    protected DistributionStatisticConfig defaultHistogramConfig() {
        return DistributionStatisticConfig.builder().expiry(Duration.ZERO).build().merge(DistributionStatisticConfig.DEFAULT);
    }
}
