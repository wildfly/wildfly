/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.lang.reflect.ReflectPermission;
import java.net.NetPermission;
import java.net.URL;
import java.util.PropertyPermission;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelApplication;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelService1;
import org.wildfly.test.integration.observability.opentelemetry.jaeger.JaegerResponse;

public abstract class BaseOpenTelemetryTest {
    protected static boolean dockerAvailable = AssumeTestGroupUtil.isDockerAvailable();

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
    protected URL url;

    static WebArchive buildBaseArchive(String name) {
        return dockerAvailable ?
                ShrinkWrap
                        .create(WebArchive.class, name + ".war")
                        .addClasses(
                                BaseOpenTelemetryTest.class,
                                OtelApplication.class,
                                OtelService1.class,
                                AssumeTestGroupUtil.class
                        )
                        .addPackage(JaegerResponse.class.getPackage())
                        .addAsManifestResource(new StringAsset("otel.sdk.disabled=false"),
                                "microprofile-config.properties")
                        .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml")
                        .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"/>"), "beans.xml")
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
                                "permissions.xml") :
                AssumeTestGroupUtil.emptyWar(name);
    }

    @BeforeClass
    public static void checkForDocker() {
        AssumeTestGroupUtil.assumeDockerAvailable();
    }
}
