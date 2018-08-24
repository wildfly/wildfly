/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package jboss.as.test.integration.microprofile.opentracing;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.test.shared.ServerReload.executeReloadAndWaitForCompletion;

import java.net.URI;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import jboss.as.test.integration.microprofile.opentracing.application.SimpleJaxRs;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests integration with eclipse-microprofile-opentracing specification, see https://github.com/eclipse/microprofile-opentracing.
 *
 * @author Jan Stourac <jstourac@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OnOffOpenTracingTestCase {

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    private URL url;

    private static final String DEPLOYMENT = "deployment";

    private static final String WEB_XML =
              "<web-app version=\"3.1\" xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n"
            + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "        xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd\">\n"
            + "    <servlet-mapping>\n"
            + "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
            + "        <url-pattern>/rest/*</url-pattern>\n"
            + "    </servlet-mapping>\n"
            + "</web-app>";

    @Deployment(name = DEPLOYMENT)
    public static Archive createContainer1Deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClass(SimpleJaxRs.class);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        war.addAsWebInfResource(new StringAsset(WEB_XML), "web.xml");

        return war;
    }

    private AutoCloseable serverSnapshot;

    @Before
    public void before() throws Exception {
        serverSnapshot = ServerSnapshot.takeSnapshot(managementClient);
    }

    @After
    public void after() throws Exception {
        serverSnapshot.close();
    }

    private void opentracingSubsystem(String operationName, ModelControllerClient client) throws Exception {
        final String OT_SUBSYSTEM_NAME = "microprofile-opentracing-smallrye";

        // remove the OpenTracing subsystem
        ModelNode operation = createOpNode("subsystem=" + OT_SUBSYSTEM_NAME, operationName);
        Utils.applyUpdate(operation, client);

        executeReloadAndWaitForCompletion(client, 100000);
    }

    private void enableServletStackTraceOnError() throws Exception {
        ModelControllerClient client = managementClient.getControllerClient();

        ModelNode operation = createOpNode("subsystem=undertow/servlet-container=default",
                ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        operation.get("name").set("stack-trace-on-error");
        operation.get("value").set("all");

        Utils.applyUpdate(operation, client);

        executeReloadAndWaitForCompletion(client, 100000);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testOpenTracingMultinodeServer() throws Exception {
        URI requestUri = new URI(url.toString() + "rest" + SimpleJaxRs.GET_TRACER);

        // In case the server does not listen on localhost, enable stack-traces for deployments to be included in the
        // HTTP response as we check its content later in this test case.
        enableServletStackTraceOnError();

        // Perform request to the deployment on server where OpenTracing is enabled. Deployment has to have a JaegerTracer instance available.
        String response = Utils.makeCall(requestUri, 200);
        Assert.assertNotEquals(SimpleJaxRs.TRACER_NOT_INITIALIZED, response);
        Assert.assertTrue(response.contains("Tracer instance is: JaegerTracer"));

        // Remove OpenTracing subsystem to disable it.
        opentracingSubsystem(ModelDescriptionConstants.REMOVE, managementClient.getControllerClient());

        try {
            // Perform request to the deployment on server where OpenTracing is disabled. Deployment must not have a Tracer instance available.
            // Actually 500 internal server error is responded with NoClassDefFoundError.
            response = Utils.makeCall(requestUri, 500);
            Assert.assertTrue(response.contains("java.lang.NoClassDefFoundError: Lio/opentracing/Tracer"));
        } finally {
            opentracingSubsystem(ModelDescriptionConstants.ADD, managementClient.getControllerClient());
        }

        // And again perform request to the deployment on the server where OpenTracing is enabled.
        response = Utils.makeCall(requestUri, 200);
        Assert.assertNotEquals(SimpleJaxRs.TRACER_NOT_INITIALIZED, response);
        Assert.assertTrue(response.contains("Tracer instance is: JaegerTracer"));
    }

}
