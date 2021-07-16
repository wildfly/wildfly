/*
 * Copyright 2020 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.microprofile.opentracing;

import io.jaegertracing.internal.metrics.Counter;
import io.jaegertracing.internal.metrics.Gauge;
import io.jaegertracing.internal.metrics.Timer;
import io.jaegertracing.spi.MetricsFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class WildflyJaegerMetricsFactory implements MetricsFactory {

    static class Metric implements Counter, Gauge, Timer {
        private final AtomicLong value = new AtomicLong();

        public long getValue() {
            return value.longValue();
        }

        @Override
        public void inc(long delta) {
            value.addAndGet(delta);
        }

        @Override
        public void update(long amount) {
            value.addAndGet(amount);
        }

        @Override
        public void durationMicros(long time) {
            value.addAndGet(time);
        }
    }

    @Override
    public Counter createCounter(String name, Map<String, String> tags) {
        return new Metric();
    }

    @Override
    public Gauge createGauge(String name, Map<String, String> tags) {
        return new Metric();
    }

    @Override
    public Timer createTimer(String name, Map<String, String> tags) {
        return new Metric();
    }
}
