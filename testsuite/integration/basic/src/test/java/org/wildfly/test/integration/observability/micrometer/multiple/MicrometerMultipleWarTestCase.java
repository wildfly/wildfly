/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.multiple;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.integration.observability.micrometer.multiple.application.DuplicateMetricResource1;
import org.wildfly.test.integration.observability.micrometer.multiple.application.DuplicateMetricResource2;

public class MicrometerMultipleWarTestCase extends BaseMicrometerMultipleTestCase {
    @Deployment(name = SERVICE_ONE, order = 1, testable = false)
    public static WebArchive createDeployment1() {
        return ShrinkWrap.create(WebArchive.class, SERVICE_ONE + ".war")
                .addClasses(JaxRsActivator.class, DuplicateMetricResource1.class)
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");

    }

    @Deployment(name = SERVICE_TWO, order = 2, testable = false)
    public static WebArchive createDeployment2() {
        return ShrinkWrap.create(WebArchive.class, SERVICE_TWO + ".war")
                .addClasses(JaxRsActivator.class, DuplicateMetricResource2.class)
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    @Test
    @InSequence(1)
    public void checkPingCount(@ArquillianResource @OperateOnDeployment(SERVICE_ONE) URL serviceOne,
                               @ArquillianResource @OperateOnDeployment(SERVICE_TWO) URL serviceTwo)
            throws URISyntaxException, InterruptedException {
        makeRequests(new URI(String.format("%s/%s", serviceOne, DuplicateMetricResource1.TAG)));
        makeRequests(new URI(String.format("%s/%s", serviceTwo, DuplicateMetricResource2.TAG)));

        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> results = otelCollector.getMetricsByName(prometheusMetrics,
                    DuplicateMetricResource1.METER_NAME + "_total"); // Adjust for Prometheus naming conventions

            assertEquals(2, results.size());
            results.forEach(r -> assertEquals("" + REQUEST_COUNT, r.getValue()));
        });
    }
}
