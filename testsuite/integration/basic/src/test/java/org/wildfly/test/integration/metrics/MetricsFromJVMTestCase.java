/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.metrics;

import static org.junit.Assert.assertTrue;
import static org.wildfly.test.integration.metrics.MetricsHelper.getMetricValueFromPrometheusOutput;
import static org.wildfly.test.integration.metrics.MetricsHelper.getPrometheusMetrics;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test JVM metrics with the base "metrics" subsystem.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MetricsFromJVMTestCase {

    // Use an empty deployment as the test deals with base metrics only
    @Deployment(name = "MetricsFromJVMTestCase", managed = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MetricsFromJVMTestCase.war");
        return war;
    }

    @BeforeClass
    public static void skipNonPreview() {
        AssumeTestGroupUtil.assumeNotWildFlyPreview();
    }

    @ContainerResource
    ManagementClient managementClient;

    @Test
    public void testBaseMetrics() throws Exception {
        long start = System.currentTimeMillis();
        long sleep = 50;

        String metrics = getPrometheusMetrics(managementClient, true);
        System.out.println("metrics = " + metrics);
        double uptime1 = getMetricValueFromPrometheusOutput(metrics, "base_jvm_uptime");
        assertTrue(uptime1 > 0);

        Thread.sleep(sleep);

        metrics = getPrometheusMetrics(managementClient, true);
        double uptime2 = getMetricValueFromPrometheusOutput(metrics, "base_jvm_uptime");
        assertTrue(uptime2 > 0);

        long interval = System.currentTimeMillis() - start;

        double uptimeDeltaInMilliseconds = (uptime2 - uptime1) * 1000;

        assertTrue( uptime2 > uptime1);
        assertTrue(uptimeDeltaInMilliseconds >= sleep);
        assertTrue(uptimeDeltaInMilliseconds <= interval);
    }
}
