/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer.registry;

import static org.wildfly.extension.micrometer.registry.ApplicationRegistry.TAG_WF_DEPLOYMENT;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadDeadlockMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.search.RequiredSearch;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.jboss.resteasy.spi.InternalServerErrorException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ApplicationRegistryTest {

    public static final String METER_APP = "appCounter";
    public static final String METER_JVM_MEMORY = "jvm.memory.used";
    public static final String METER_SYSTEM = "systemCounter";

    public static final String DEPLOYMENT1 = "app1";
    public static final String DEPLOYMENT2 = "app2";

    private CompositeMeterRegistry systemRegistry;
    private ApplicationRegistry appRegistry1;
    private ApplicationRegistry appRegistry2;

    @Before
    public void setUp() {
        systemRegistry = new CompositeMeterRegistry();
        systemRegistry.add(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT));
        Counter systemCounter = systemRegistry.counter(METER_SYSTEM);
        systemCounter.increment(10);

        // Register lots of metrics to sift through
        new ClassLoaderMetrics().bindTo(systemRegistry);
        new JvmMemoryMetrics().bindTo(systemRegistry);
        new JvmGcMetrics().bindTo(systemRegistry);
        new ProcessorMetrics().bindTo(systemRegistry);
        new JvmThreadMetrics().bindTo(systemRegistry);
        new JvmThreadDeadlockMetrics().bindTo(systemRegistry);

        appRegistry1 = registerTestMeters(DEPLOYMENT1);
        appRegistry2 = registerTestMeters(DEPLOYMENT2);
    }

    private ApplicationRegistry registerTestMeters(String deploymentName) {
        ApplicationRegistry registry = new ApplicationRegistry(deploymentName, systemRegistry);
        var counter = registry.counter(METER_APP);
        counter.increment(5);
        registry.timer(deploymentName + "_timer");
        registry.gauge(deploymentName + "_gauge", new ArrayList<String>(), List::size);
        registry.summary(deploymentName + "_summary");
        registry.more().counter(deploymentName + "_function_counter1", Tags.empty(), new ArrayList<String>(), List::size);
        registry.more().counter(deploymentName + "_function_counter2", Tags.empty(), 49152);
        registry.more().longTaskTimer(deploymentName + "_long_task_timer", Tags.empty());
        registry.more().timeGauge(deploymentName + "_time_gauge", Tags.empty(), new AtomicInteger(4000), TimeUnit.MILLISECONDS, AtomicInteger::get);
        // A List doesn't make much sense for a timer, but, for testing purposes, we just need an object and methods to call on it.
        registry.more().timer(deploymentName + "_function_timer", Tags.empty(), new ArrayList<String>(), List::size, List::size, TimeUnit.NANOSECONDS);

        return registry;
    }

    @Test
    public void verifySystemMeters() {
        Assert.assertNotNull(systemRegistry.find(METER_SYSTEM).meter());
        Assert.assertNotNull(systemRegistry.find(METER_JVM_MEMORY).meter());
    }

    @Test
    public void verifyAppMeterIsolation() {
        Counter counter1 = appRegistry1.counter(METER_SYSTEM);
        Counter counter2 = appRegistry2.counter(METER_SYSTEM);

        Counter counter3 = appRegistry1.counter(METER_APP);
        Counter counter4 = appRegistry2.counter(METER_APP);

        // System metrics should not be visible
        Assert.assertEquals(0d, counter1.count(), 0.0);
        assertMetricBelongsToDeployment(counter1.getId(), DEPLOYMENT1);
        Assert.assertEquals(0d, counter2.count(), 0.0);

        Assert.assertEquals(5d, counter3.count(), 0.0);
        assertMetricBelongsToDeployment(counter3.getId(), DEPLOYMENT1);

        Assert.assertEquals(5, counter4.count(), 0.0);
        assertMetricBelongsToDeployment(counter4.getId(), DEPLOYMENT2);
    }

    @Test
    public void testFind() {
        // Should find meters from correct registry
        Assert.assertNotNull(appRegistry1.find(METER_APP).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT1).counter());
        Assert.assertNotNull(appRegistry2.find(METER_APP).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT2).counter());
        // Should NOT find meters from other registry
        Assert.assertNull(appRegistry1.find(METER_APP).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT2).counter());
        Assert.assertNull(appRegistry2.find(METER_APP).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT1).counter());
    }

    @Test
    public void testFindForSystemMetrics() {
        // System metrics should not be visible
        Assert.assertNull(appRegistry1.find(METER_JVM_MEMORY).meter());
        Assert.assertNull(appRegistry2.find(METER_JVM_MEMORY).meter());
    }

    @Test
    public void testGetForSystemMetrics() {
        Assert.assertThrows(MeterNotFoundException.class, () -> appRegistry1.get(METER_SYSTEM).meter());
        Assert.assertThrows(MeterNotFoundException.class, () -> appRegistry1.get(METER_JVM_MEMORY).meter());
        Assert.assertThrows(MeterNotFoundException.class, () -> appRegistry2.get(METER_SYSTEM).meter());
        Assert.assertThrows(MeterNotFoundException.class, () -> appRegistry2.get(METER_JVM_MEMORY).meter());
    }

    @Test
    public void testGet() {
        // Should succeed
        assertMetricBelongsToDeployment(appRegistry1.get(METER_APP).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT1).meter().getId(), DEPLOYMENT1);
        // Should fail
        try {
            appRegistry2.get(METER_APP).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT1).counter();
            Assert.fail("Meter from other register should not be visible.");
        } catch (MeterNotFoundException e) {
            //
        }

        // Should succeed
        assertMetricBelongsToDeployment(appRegistry2.get(METER_APP).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT2).meter().getId(), DEPLOYMENT2);
        // Should fail
        try {
            appRegistry1.get(METER_APP).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT2).counter();
            Assert.fail("Meter from other register should not be visible.");
        } catch (MeterNotFoundException e) {
            //
        }
    }

    @Test
    public void testGetMeters() {
        Assert.assertTrue(appRegistry1.getMeters().stream()
                .noneMatch(m -> containsWrongDeploymentTag(m.getId(), DEPLOYMENT1)));
        Assert.assertTrue(appRegistry2.getMeters().stream()
                .noneMatch(m -> containsWrongDeploymentTag(m.getId(), DEPLOYMENT2)));
    }

    @Test
    public void testGetMetersForSystemMetrics() {
        Assert.assertTrue(appRegistry1.getMeters().stream()
                .noneMatch(m -> m.getId().getTag(TAG_WF_DEPLOYMENT) == null));
        Assert.assertTrue(appRegistry2.getMeters().stream()
                .noneMatch(m -> m.getId().getTag(TAG_WF_DEPLOYMENT) == null));
    }

    @Test
    public void testForEachMeter() {
        appRegistry1.forEachMeter(m -> assertMetricBelongsToDeployment(m.getId(), DEPLOYMENT1));
        appRegistry2.forEachMeter(m -> assertMetricBelongsToDeployment(m.getId(), DEPLOYMENT2));
    }

    @Test
    public void testRemoveByMeter() {
        Assert.assertNotNull(appRegistry1.remove(appRegistry1.counter(METER_APP)));
        Assert.assertNull(appRegistry1.remove(appRegistry2.counter(METER_APP)));
    }

    @Test
    public void testRemoveById() {
        Meter.Id good = new Meter.Id(METER_APP, Tags.of(TAG_WF_DEPLOYMENT, DEPLOYMENT1), null, null, Meter.Type.COUNTER);
        Meter.Id other = new Meter.Id(METER_APP, Tags.of(TAG_WF_DEPLOYMENT, DEPLOYMENT2), null, null, Meter.Type.COUNTER);
        Meter.Id system = new Meter.Id(METER_JVM_MEMORY, Tags.empty(), null, null, Meter.Type.COUNTER);

        Assert.assertNotNull(appRegistry1.remove(good));
        Assert.assertNull(appRegistry1.remove(other));
        Assert.assertNull(appRegistry1.remove(system));
        Assert.assertNull(appRegistry2.remove(system));
    }

    @Test
    public void testRemoveByPreFilterId() {
        Meter.Id good = new Meter.Id(METER_APP, Tags.of(TAG_WF_DEPLOYMENT, DEPLOYMENT1), null, null, Meter.Type.COUNTER);
        Meter.Id other = new Meter.Id(METER_APP, Tags.of(TAG_WF_DEPLOYMENT, DEPLOYMENT2), null, null, Meter.Type.COUNTER);
        Meter.Id system = new Meter.Id(METER_JVM_MEMORY, Tags.empty(), null, null, Meter.Type.COUNTER);

        Assert.assertNotNull(appRegistry1.removeByPreFilterId(good));
        Assert.assertNull(appRegistry1.removeByPreFilterId(other));
        Assert.assertNull(appRegistry1.remove(system));
        Assert.assertNull(appRegistry2.remove(system));
    }

    @Test
    public void testCounterIsolation() {
        checkMeterVisibility(() -> appRegistry1.counter("app2_counter"), RequiredSearch::counter);
    }

    @Test
    public void testTimerIsolation() {
        checkMeterVisibility(() -> appRegistry1.timer("app2_timer"), RequiredSearch::timer);
    }

    @Test
    public void testGaugeIsolation() {
        checkMeterVisibility(() -> {
            appRegistry1.gauge("app2_gauge", new ArrayList<String>(), List::size);
            return appRegistry1.get("app2_gauge").gauge();
        }, RequiredSearch::gauge);
    }

    @Test
    public void testSummaryIsolation() {
        checkMeterVisibility(() -> appRegistry1.summary("app2_summary"), RequiredSearch::summary);
    }

    @Test
    public void testFunctionCounterIsolation() {
        checkMeterVisibility(() -> {
            appRegistry1.more().counter("app2_function_counter1", Tags.empty(), new ArrayList<String>(), List::size);
            return appRegistry1.get("app2_function_counter1").functionCounter();
        }, RequiredSearch::gauge);
        checkMeterVisibility(() -> {
            appRegistry1.more().counter("app2_function_counter2", Tags.empty(), 49152);
            return appRegistry1.get("app2_function_counter2").functionCounter();
        }, RequiredSearch::gauge);
    }

    @Test
    public void testLongTaskTimerIsolation() {
        checkMeterVisibility(() -> {
            appRegistry1.more().longTaskTimer("app2_long_task_timer", Tags.empty());
            return appRegistry1.get("app2_long_task_timer").longTaskTimer();
        }, RequiredSearch::gauge);
    }

    @Test
    public void testTimeGaugeIsolation() {
        checkMeterVisibility(() -> {
            appRegistry1.more().timeGauge("app2_time_gauge", Tags.empty(), new AtomicInteger(4000), TimeUnit.MILLISECONDS, AtomicInteger::get);
            return appRegistry1.get("app2_time_gauge").timeGauge();
        }, RequiredSearch::gauge);
    }

    @Test
    public void testFunctionTimerIsolation() {
        checkMeterVisibility(() -> {
            appRegistry1.more().timer("app2_function_timer", Tags.empty(), new ArrayList<String>(), List::size, List::size, TimeUnit.NANOSECONDS);
            return appRegistry1.get("app2_function_timer").functionTimer();
        }, RequiredSearch::gauge);
    }

    @Test
    public void testClear() {
        Assert.assertFalse(systemRegistry.getMeters().isEmpty());
        Assert.assertFalse(appRegistry1.getMeters().isEmpty());
        Assert.assertFalse(appRegistry2.getMeters().isEmpty());

        appRegistry1.clear();

        Assert.assertFalse(systemRegistry.getMeters().isEmpty());
        Assert.assertTrue(appRegistry1.getMeters().isEmpty());
        Assert.assertFalse(appRegistry2.getMeters().isEmpty());
    }

    // Tests for Builder pattern registration (WFLY-21339)

    @Test
    public void testCounterBuilderRegistersToParent() {
        String meterName = "builder_counter_test";
        Counter counter = Counter.builder(meterName)
                .description("Test counter via builder")
                .register(appRegistry1);

        // Verify the counter is registered in the parent registry with deployment tag
        Assert.assertNotNull("Counter should be in parent registry",
                systemRegistry.find(meterName).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT1).counter());
        assertMetricBelongsToDeployment(counter.getId(), DEPLOYMENT1);
    }

    @Test
    public void testGaugeBuilderRegistersToParent() {
        String meterName = "builder_gauge_test";
        AtomicInteger gaugeValue = new AtomicInteger(42);
        Gauge.builder(meterName, gaugeValue, AtomicInteger::get)
                .description("Test gauge via builder")
                .register(appRegistry1);

        // Verify the gauge is registered in the parent registry with deployment tag
        Gauge gauge = systemRegistry.find(meterName).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT1).gauge();
        Assert.assertNotNull("Gauge should be in parent registry", gauge);
        Assert.assertEquals(42.0, gauge.value(), 0.0);
    }

    @Test
    public void testTimerBuilderRegistersToParent() {
        String meterName = "builder_timer_test";
        Timer timer = Timer.builder(meterName)
                .description("Test timer via builder")
                .register(appRegistry1);

        // Verify the timer is registered in the parent registry with deployment tag
        Assert.assertNotNull("Timer should be in parent registry",
                systemRegistry.find(meterName).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT1).timer());
        assertMetricBelongsToDeployment(timer.getId(), DEPLOYMENT1);
    }

    @Test
    public void testBuilderPatternIsolation() {
        String meterName = "builder_isolation_test";

        // Register counter via builder in app1
        Counter counter1 = Counter.builder(meterName)
                .register(appRegistry1);
        counter1.increment(10);

        // Register counter via builder in app2
        Counter counter2 = Counter.builder(meterName)
                .register(appRegistry2);
        counter2.increment(20);

        // Verify isolation - each app sees only its own counter
        Assert.assertEquals(10.0, appRegistry1.get(meterName).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT1).counter().count(), 0.0);
        Assert.assertEquals(20.0, appRegistry2.get(meterName).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT2).counter().count(), 0.0);

        // Verify app1 cannot see app2's counter
        Assert.assertThrows(MeterNotFoundException.class,
                () -> appRegistry1.get(meterName).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT2).counter());
    }

    @Test
    public void testDistributionSummaryBuilderRegistersToParent() {
        String meterName = "builder_summary_test";
        DistributionSummary summary = DistributionSummary.builder(meterName)
                .description("Test summary via builder")
                .register(appRegistry1);

        // Verify the summary is registered in the parent registry with deployment tag
        Assert.assertNotNull("DistributionSummary should be in parent registry",
                systemRegistry.find(meterName).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT1).summary());
        assertMetricBelongsToDeployment(summary.getId(), DEPLOYMENT1);
    }

    @Test
    public void testFunctionCounterBuilderRegistersToParent() {
        String meterName = "builder_function_counter_test";
        AtomicInteger counterValue = new AtomicInteger(100);
        FunctionCounter functionCounter = FunctionCounter.builder(meterName, counterValue, AtomicInteger::get)
                .description("Test function counter via builder")
                .register(appRegistry1);

        // Verify the function counter is registered in the parent registry with deployment tag
        Assert.assertNotNull("FunctionCounter should be in parent registry",
                systemRegistry.find(meterName).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT1).functionCounter());
        assertMetricBelongsToDeployment(functionCounter.getId(), DEPLOYMENT1);
    }

    @Test
    public void testFunctionTimerBuilderRegistersToParent() {
        String meterName = "builder_function_timer_test";
        AtomicInteger count = new AtomicInteger(5);
        FunctionTimer functionTimer = FunctionTimer.builder(meterName, count,
                        AtomicInteger::get, AtomicInteger::doubleValue, TimeUnit.MILLISECONDS)
                .description("Test function timer via builder")
                .register(appRegistry1);

        // Verify the function timer is registered in the parent registry with deployment tag
        Assert.assertNotNull("FunctionTimer should be in parent registry",
                systemRegistry.find(meterName).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT1).functionTimer());
        assertMetricBelongsToDeployment(functionTimer.getId(), DEPLOYMENT1);
    }

    @Test
    public void testLongTaskTimerBuilderRegistersToParent() {
        String meterName = "builder_long_task_timer_test";
        LongTaskTimer longTaskTimer = LongTaskTimer.builder(meterName)
                .description("Test long task timer via builder")
                .register(appRegistry1);

        // Verify the long task timer is registered in the parent registry with deployment tag
        Assert.assertNotNull("LongTaskTimer should be in parent registry",
                systemRegistry.find(meterName).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT1).longTaskTimer());
        assertMetricBelongsToDeployment(longTaskTimer.getId(), DEPLOYMENT1);
    }

    @Test
    public void testTimeGaugeBuilderRegistersToParent() {
        String meterName = "builder_time_gauge_test";
        AtomicInteger timeValue = new AtomicInteger(5000);
        TimeGauge timeGauge = TimeGauge.builder(meterName, timeValue, TimeUnit.MILLISECONDS, AtomicInteger::get)
                .description("Test time gauge via builder")
                .register(appRegistry1);

        // Verify the time gauge is registered in the parent registry with deployment tag
        Assert.assertNotNull("TimeGauge should be in parent registry",
                systemRegistry.find(meterName).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT1).timeGauge());
        assertMetricBelongsToDeployment(timeGauge.getId(), DEPLOYMENT1);
    }

    @Test
    public void testMeterBuilderRegistersToParent() {
        String meterName = "builder_meter_test";
        Meter meter = Meter.builder(meterName, Meter.Type.OTHER, List.of(new Measurement(() -> 42.0, Statistic.VALUE)))
                .description("Test meter via builder")
                .register(appRegistry1);

        // Verify the meter is registered in the parent registry with deployment tag
        Assert.assertNotNull("Meter should be in parent registry",
                systemRegistry.find(meterName).tag(TAG_WF_DEPLOYMENT, DEPLOYMENT1).meter());
        assertMetricBelongsToDeployment(meter.getId(), DEPLOYMENT1);
    }

    private boolean containsWrongDeploymentTag(Meter.Id id, String deploymentName) {
        // If the tag doesn't match the current deployment, whether it's missing or different, it should be denied
        return !deploymentName.equals(id.getTag(TAG_WF_DEPLOYMENT));

    }

    private <T extends Meter> void checkMeterVisibility(Supplier<T> producer,
                                                        Function<RequiredSearch, T> searchHandler) {
        T meter = producer.get();

        if (containsWrongDeploymentTag(meter.getId(), DEPLOYMENT1)) {
            throw new RuntimeException("'Other' meter marker tag should match. Meters are leaking between deployments.");
        }

        try {
            searchHandler.apply(appRegistry1.get(meter.getId().getName()).tags(Tags.of(TAG_WF_DEPLOYMENT, DEPLOYMENT2)));
            throw new InternalServerErrorException("Other meter should not be found.");
        } catch (MeterNotFoundException e) {
            //
        }
    }

    private void assertMetricBelongsToDeployment(Meter.Id id, String deploymentName) {
        Assert.assertFalse("Meter " + id + " contains the wrong deployment tag",
                containsWrongDeploymentTag(id, deploymentName));
    }
}
