/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.lang.reflect.ReflectPermission;
import java.net.MalformedURLException;
import java.net.NetPermission;
import java.util.PropertyPermission;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.arquillian.testcontainers.api.Testcontainer;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AssumptionViolatedException;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelService1;

@RunWith(Arquillian.class)
@DockerRequired(AssumptionViolatedException.class)
public abstract class BaseOpenTelemetryTest {
    @Testcontainer
    protected OpenTelemetryCollectorContainer otelCollector;

    private static final String MP_CONFIG = "otel.sdk.disabled=false\n" +
//        "otel.metrics.exporter=otlp\n" +
//        "otel.traces.exporter=otlp\n" +
        "otel.metric.export.interval=100";

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
            // Some of the classes used in testing do things that break when the Security Manager is installed
            .addAsManifestResource(createPermissionsXmlAsset(
                    new RuntimePermission("getClassLoader"),
                    new RuntimePermission("getProtectionDomain"),
                    new RuntimePermission("getenv.*"),
                    new RuntimePermission("setDefaultUncaughtExceptionHandler"),
                    new RuntimePermission("modifyThread"),
                    new ReflectPermission("suppressAccessChecks"),
                    new NetPermission("getProxySelector"),
                    new PropertyPermission("*", "read, write")),
                "permissions.xml");
    }

    protected String getDeploymentUrl(String deploymentName) throws MalformedURLException {
        return TestSuiteEnvironment.getHttpUrl() + "/" + deploymentName + "/";
    }
}
