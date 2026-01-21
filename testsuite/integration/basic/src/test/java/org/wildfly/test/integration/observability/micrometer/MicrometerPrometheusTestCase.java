/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import static org.wildfly.test.integration.observability.setuptask.PrometheusSetupTask.PROMETHEUS_REGISTRY_ADDRESS;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.integration.observability.setuptask.PrometheusSetupTask;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

@ServerSetup({StabilityServerSetupSnapshotRestoreTasks.Community.class, PrometheusSetupTask.class})
public class MicrometerPrometheusTestCase extends BaseMicrometerTest {

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "micrometer-prometheus.war")
                .addClasses(JaxRsActivator.class, MicrometerResource.class)
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    @Test
    public void basicPrometheusTest() throws Exception {
        makeRequests();

        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> results = otelCollector.getMetricsByName(prometheusMetrics, "demo_counter_total"); // Adjust for Prometheus naming conventions

            Assert.assertEquals(1, results.size());
            results.forEach(r -> Assert.assertEquals("" + REQUEST_COUNT, r.getValue()));

            Assert.assertNotEquals(0, otelCollector.getMetricsByName(prometheusMetrics, "demo_timer_milliseconds_count").size());
        });
    }

    @Test
    public void securedPrometheusTest() throws Exception {
        setPrometheusSecurity(true);
        makeRequests();

        List<PrometheusMetric> metrics = fetchPrometheusMetrics(false);
        Assert.assertTrue("Metrics should not be exposed without authentication",
                metrics.stream().noneMatch(m -> m.getKey().equals("demo_counter_total")));

        metrics = fetchPrometheusMetrics(true);
        Assert.assertTrue("'demo_counter_total' is expected",
                metrics.stream().anyMatch(m -> m.getKey().equals("demo_counter_total")));

        setPrometheusSecurity(false);
        makeRequests();
        metrics = fetchPrometheusMetrics(false);
        Assert.assertTrue("'demo_counter_total' is expected",
                metrics.stream().anyMatch(m -> m.getKey().equals("demo_counter_total")));
    }

    private void setPrometheusSecurity(boolean enabled) throws Exception {
        executeOperation(Operations.createWriteAttributeOperation(PROMETHEUS_REGISTRY_ADDRESS, "security-enabled", enabled));
        ServerReload.reloadIfRequired(managementClient);
    }
}
