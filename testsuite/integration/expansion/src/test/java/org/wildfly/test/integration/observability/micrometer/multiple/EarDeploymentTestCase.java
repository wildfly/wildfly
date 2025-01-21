/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.multiple;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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

    @Deployment(name = ENTERPRISE_APP, testable = false)
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


        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> results = getMetricsByName(prometheusMetrics,
                    DuplicateMetricResource1.METER_NAME + "_total"); // Adjust for Prometheus naming conventions

            Assert.assertEquals(2, results.size());
            results.forEach(r -> Assert.assertEquals("" + REQUEST_COUNT, r.getValue()));
        });
    }
}