/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.multiple;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.micrometer.multiple.application.DuplicateMetricResource1;
import org.wildfly.test.integration.observability.micrometer.multiple.application.DuplicateMetricResource2;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;

public class EarDeploymentTestCase extends BaseMultipleTestCase {
    protected static final String ENTERPRISE_APP = "enterprise-app";

    @Deployment(name = ENTERPRISE_APP)
    public static EnterpriseArchive createDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, ENTERPRISE_APP + ".ear")
                .addAsModule(MultipleWarTestCase.createDeployment1())
                .addAsModule(MultipleWarTestCase.createDeployment2())
                .setApplicationXML(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<application xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                        "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "       xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/application_10.xsd\" " +
                        "       version=\"10\">\n"
                        + "  <display-name>metrics</display-name>\n"
                        + "  <module>\n"
                        + "    <web>\n"
                        + "      <web-uri>" + SERVICE_ONE + ".war</web-uri>\n"
                        + "    </web>\n"
                        + "  </module>\n"
                        + "  <module>\n"
                        + "    <web>\n"
                        + "      <web-uri>" + SERVICE_TWO + ".war</web-uri>\n"
                        + "    </web>\n"
                        + "  </module>\n"
                        + "</application>"));
    }

    @Test
    public void dataTest(@ArquillianResource @OperateOnDeployment(ENTERPRISE_APP) URL earUrl)
            throws URISyntaxException, InterruptedException {
        makeRequests(new URI(String.format("%s/%s/%s/%s", earUrl, ENTERPRISE_APP, SERVICE_ONE, DuplicateMetricResource1.TAG)));
        makeRequests(new URI(String.format("%s/%s/%s/%s", earUrl, ENTERPRISE_APP, SERVICE_TWO, DuplicateMetricResource2.TAG)));

        List<PrometheusMetric> results = Collections.emptyList();

        int attemptCount = 0;

        while (attemptCount < 10) {
            attemptCount++;

            results = getMetricsByName(
                    otelCollector.fetchMetrics(DuplicateMetricResource1.METER_NAME),
                    DuplicateMetricResource1.METER_NAME + "_total"); // Adjust for Prometheus naming conventions
            // On occasion, it seems that the test will grab the metrics between updates, so we get the metric for app 1,
            // but app 2 has not been published yet. We loop here, then, for a short while to give the server time to
            // publish the metrics it has. After that short while, we break out and fail.
            if (results.size() == 2) {
                break;
            } else {
                Thread.sleep(500);
            }
        }
        Assert.assertEquals(2, results.size());
        results.forEach(r -> Assert.assertEquals("" + REQUEST_COUNT, r.getValue()));
    }
}