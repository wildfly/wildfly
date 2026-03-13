/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.observability.collector.InMemoryCollector;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetrySetupTask;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelService1;

@RunWith(Arquillian.class)
@ServerSetup(OpenTelemetrySetupTask.class)
public abstract class BaseOpenTelemetryTest {
    public static InMemoryCollector server = InMemoryCollector.getInstance();

    private static final String MP_CONFIG = "otel.sdk.disabled=false\n" +
            // Lower the interval from 60 seconds to 2 seconds
            "otel.metric.export.interval=2000";

    static WebArchive buildBaseArchive(String name) {
        return ShrinkWrap
            .create(WebArchive.class, name + ".war")
            .addClasses(
                BaseOpenTelemetryTest.class,
                JaxRsActivator.class,
                OtelService1.class,
                OtelMetricResource.class
            )
            .addPackage(JaegerResponse.class.getPackage())
            .addAsManifestResource(new StringAsset(MP_CONFIG), "microprofile-config.properties")
            .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml")
            ;
    }

    protected String getDeploymentUrl(String deploymentName) throws MalformedURLException {
        return TestSuiteEnvironment.getHttpUrl() + "/" + deploymentName + "/";
    }

    protected void makeRequests(URL url, int count, int expectedStatus) throws URISyntaxException {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(url.toURI());
            for (int i = 0; i < count; i++) {
                Response response = target.request().get();
                Assert.assertEquals(expectedStatus, response.getStatus());
            }
        }
    }
}
