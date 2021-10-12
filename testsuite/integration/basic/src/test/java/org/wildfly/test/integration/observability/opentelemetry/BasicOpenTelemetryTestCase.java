/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.observability.opentelemetry;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.net.SocketPermission;
import java.security.SecurityPermission;

import javax.inject.Inject;
import javax.management.MBeanPermission;
import javax.management.MBeanServerPermission;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(OpenTelemetrySetupTask.class)
public class BasicOpenTelemetryTestCase {
    @Inject
    private Tracer tracer;

    @Inject
    private OpenTelemetry openTelemetry;

    private static final String WEB_XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<web-app xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://java.sun.com/xml/ns/javaee\"\n"
            + "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n"
            + "         metadata-complete=\"false\" version=\"3.0\">\n"
            + "    <servlet-mapping>\n"
            + "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
            + "        <url-pattern>/*</url-pattern>\n"
            + "    </servlet-mapping>"
            + "</web-app>";

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, BasicOpenTelemetryTestCase.class.getSimpleName() + ".war")
                .addClasses(OpenTelemetrySetupTask.class, ServerSetupTask.class)
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
//                .addClass(BasicOpenTelemetryTestCase.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        // Required for the ClientBuilder.newBuilder() so the ServiceLoader will work
                        new FilePermission("<<ALL FILES>>", "read"),
                        // Required for com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider. During <init> there is a
                        // reflection test to check for JAXRS 2.0.
                        new RuntimePermission("accessDeclaredMembers"),
                        // Required for the client to connect
                        new SocketPermission(TestSuiteEnvironment.getHttpAddress() + ":" +
                                TestSuiteEnvironment.getHttpPort(), "connect,resolve"),
                        new SecurityPermission("insertProvider"),
                        new MBeanServerPermission("createMBeanServer"),
                        new MBeanPermission("*", "registerMBean, unregisterMBean, invoke"),
                        new RuntimePermission("getClassLoader"),
                        new RuntimePermission("modifyThread"),
                        new RuntimePermission("setContextClassLoader")
                ), "permissions.xml");
    }

    @Test
    public void hasDefaultInjectedOpenTelemetry() {
        Assert.assertNotNull(openTelemetry);
    }

    @Test
    public void hasDefaultInjectedTracer() {
        Assert.assertNotNull(tracer);
    }

    @Test
    public void restClientHasFilterAdded() {
        Client client = ClientBuilder.newClient();
        org.wildfly.common.Assert.assertTrue(client.getConfiguration().getClasses().stream()
                .map(c -> c.getCanonicalName())
                .anyMatch(n -> "org.wildfly.extension.opentelemetry.api.OpenTelemetryClientRequestFilter".equals(n)));
    }
}
