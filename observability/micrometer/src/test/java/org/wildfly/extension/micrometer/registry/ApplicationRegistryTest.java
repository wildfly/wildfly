/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer.registry;

import static org.wildfly.extension.micrometer.registry.ApplicationRegistry.TAG_WF_DEPLOYMENT;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
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

        appRegistry1 = new ApplicationRegistry(DEPLOYMENT1, systemRegistry);
        var app1Counter = appRegistry1.counter(METER_APP);
        app1Counter.increment(5);
        appRegistry1.timer("app1_timer");
        appRegistry1.gauge("app1_gauge", new ArrayList<String>(), List::size);
        appRegistry1.summary("app1_summary");

        appRegistry2 = new ApplicationRegistry(DEPLOYMENT2, systemRegistry);
        var app2Counter = appRegistry2.counter(METER_APP);
        app2Counter.increment();
        appRegistry2.timer("app2_timer");
        appRegistry2.gauge("app2_gauge", new ArrayList<String>(), List::size);
        appRegistry2.summary("app2_summary");
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
        Assert.assertEquals(1d, counter4.count(), 0.0);
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
