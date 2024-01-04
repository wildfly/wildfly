/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import jakarta.inject.Inject;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelApplication;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelService;
import org.wildfly.test.integration.observability.opentelemetry.application.TestOpenTelemetryConfig;
import org.wildfly.test.integration.observability.opentelemetry.exporter.InMemorySpanExporter;
import org.wildfly.test.integration.observability.opentelemetry.exporter.InMemorySpanExporterProvider;

import java.lang.reflect.ReflectPermission;
import java.net.NetPermission;
import java.net.URL;
import java.util.PropertyPermission;

public abstract class BaseOpenTelemetryTest {
    public static final String SERVICE_NAME = RandomStringUtils.random(15);
    private static final String WEB_XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<web-app xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://java.sun.com/xml/ns/javaee\"\n"
            + "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n"
            + "         metadata-complete=\"false\" version=\"3.0\">\n"
            + "    <servlet-mapping>\n"
            + "        <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n"
            + "        <url-pattern>/*</url-pattern>\n"
            + "    </servlet-mapping>"
            + "</web-app>";
    @ArquillianResource
    URL url;
    @Inject
    InMemorySpanExporter spanExporter;

    static WebArchive buildBaseArchive(String name) {
        String beansXml =
                "<beans bean-discovery-mode=\"all\">" +
                        "    <alternatives>\n" +
                        "        <class>" + TestOpenTelemetryConfig.class.getCanonicalName() + "</class>\n" +
                        "    </alternatives>" +
                        "</beans>";
        return ShrinkWrap
                .create(WebArchive.class, name + ".war")
                .addClasses(BaseOpenTelemetryTest.class,
                        OtelApplication.class,
                        OtelService.class,
                        OpenTelemetryConfig.class,
                        TestOpenTelemetryConfig.class,
                        InMemorySpanExporter.class,
                        RandomStringUtils.class,
                        InMemorySpanExporterProvider.class)
                .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                .addAsLibrary(ShrinkWrap.create(JavaArchive.class, "awaitility.jar")
                        .addPackages(true, "org.awaitility", "org.hamcrest")
                )
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml")
                .addAsWebInfResource(new StringAsset(beansXml), "beans.xml")
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
}
