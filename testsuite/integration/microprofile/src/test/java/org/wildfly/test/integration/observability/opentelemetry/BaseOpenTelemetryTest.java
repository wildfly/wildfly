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

import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.test.integration.observability.opentelemetry.application.JaxRsActivator;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelService1;

public abstract class BaseOpenTelemetryTest {
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

    static WebArchive buildBaseArchive(String name) {
        return ShrinkWrap
                .create(WebArchive.class, name + ".war")
                .addClasses(
                        BaseOpenTelemetryTest.class,
                        JaxRsActivator.class,
                        OtelService1.class
                )
                .addPackage(JaegerResponse.class.getPackage())
                .addAsManifestResource(new StringAsset("otel.sdk.disabled=false"),
                        "microprofile-config.properties")
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml")
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
