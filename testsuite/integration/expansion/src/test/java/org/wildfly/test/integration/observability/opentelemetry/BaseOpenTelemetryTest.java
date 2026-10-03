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
import org.arquillian.testcontainers.api.Testcontainer;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelService1;

/** Provides the shared deployment and HTTP helpers for OpenTelemetry integration tests. */
@RunWith(Arquillian.class)
@TestcontainersRequired
public abstract class BaseOpenTelemetryTest {
    @Testcontainer
    protected OpenTelemetryCollectorContainer otelCollector;

    private static final String MP_CONFIG = "otel.sdk.disabled=false\n" +
            // Lower the interval from 60 seconds to 2 seconds
            "otel.metric.export.interval=2000";

    /**
     * Builds a deployment using the shared OpenTelemetry test configuration.
     *
     * @param name the deployment name without the {@code .war} suffix
     * @return the test archive
     */
    static WebArchive buildBaseArchive(String name) {
        return buildBaseArchive(name, null);
    }

    /**
     * Builds a deployment using the shared configuration and an optional deployment service name.
     *
     * @param name the deployment name without the {@code .war} suffix
     * @param serviceName the deployment service name, or {@code null} to use the subsystem default
     * @return the test archive
     */
    static WebArchive buildBaseArchive(String name, String serviceName) {
        String deploymentConfig = serviceName == null
                ? MP_CONFIG
                : MP_CONFIG + "\notel.service.name=" + serviceName;
        return ShrinkWrap
            .create(WebArchive.class, name + ".war")
            .addClasses(
                BaseOpenTelemetryTest.class,
                JaxRsActivator.class,
                OtelService1.class,
                OtelMetricResource.class
            )
            .addPackage(JaegerResponse.class.getPackage())
            .addAsManifestResource(new StringAsset(deploymentConfig), "microprofile-config.properties")
            .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml")
            ;
    }

    /**
     * Returns the root URL for a deployment.
     *
     * @param deploymentName the deployment name
     * @return the deployment root URL
     * @throws MalformedURLException if the configured test URL is invalid
     */
    protected String getDeploymentUrl(String deploymentName) throws MalformedURLException {
        return TestSuiteEnvironment.getHttpUrl() + "/" + deploymentName + "/";
    }

    /**
     * Sends requests and verifies every response status while closing each response.
     *
     * @param url the request URL
     * @param count the number of requests to send
     * @param expectedStatus the expected HTTP status
     * @throws URISyntaxException if the supplied URL cannot be converted to a URI
     */
    protected void makeRequests(URL url, int count, int expectedStatus) throws URISyntaxException {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(url.toURI());
            for (int i = 0; i < count; i++) {
                try (Response response = target.request().get()) {
                    Assert.assertEquals(expectedStatus, response.getStatus());
                }
            }
        }
    }
}
