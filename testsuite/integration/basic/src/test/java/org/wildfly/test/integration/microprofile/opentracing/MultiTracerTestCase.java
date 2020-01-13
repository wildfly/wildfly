/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.integration.microprofile.opentracing;

import static org.jboss.as.test.shared.ServerReload.executeReloadAndWaitForCompletion;

import java.net.URL;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.opentracing.application.TracerIdentityApplication;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(MultiTracerTestCase.SetupTask.class)
public class MultiTracerTestCase {

    private static final String WEB_XML
            = "<web-app version=\"3.1\" xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n"
            + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "        xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd\">\n"
            + "    <context-param>\n"
            + "        <param-name>mp.opentracing.extensions.tracer.configuration</param-name>\n"
            + "        <param-value>jaeger-test</param-value>\n"
            + "    </context-param>\n"
            + "</web-app>";

    public static class SetupTask implements ServerSetupTask {

        private static final PathAddress OT_SUBSYSTEM = PathAddress.parseCLIStyleAddress("/subsystem=microprofile-opentracing-smallrye");

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode addTracer = Operations.createAddOperation(OT_SUBSYSTEM.append("jaeger-tracer", "jaeger-test").toModelNode());
            addTracer.get("sampler-type").set("const");
            addTracer.get("sampler-param").set(1.0D);
            ModelNode tags = addTracer.get("tags");
            tags.get("tracer-tag").set("test");
            Utils.applyUpdate(addTracer, managementClient.getControllerClient());
            tags = new ModelNode();
            tags.get("tracer-tag").set("initial");
            ModelNode updateTracer = Operations.createWriteAttributeOperation(OT_SUBSYSTEM.append("jaeger-tracer", "jaeger").toModelNode(), "tags", tags);
            Utils.applyUpdate(updateTracer, managementClient.getControllerClient());
            executeReloadAndWaitForCompletion(managementClient, 100000);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            Utils.applyUpdate(Operations.createRemoveOperation(OT_SUBSYSTEM.append("jaeger-tracer", "jaeger-test").toModelNode()), managementClient.getControllerClient());
            ModelNode updateTracer = Operations.createUndefineAttributeOperation(OT_SUBSYSTEM.append("jaeger-tracer", "jaeger").toModelNode(), "tags");
            Utils.applyUpdate(updateTracer, managementClient.getControllerClient());
            executeReloadAndWaitForCompletion(managementClient, 100000);
        }

    }

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    @OperateOnDeployment("ServiceOne.war")
    private URL serviceOneUrl;

    @ArquillianResource
    @OperateOnDeployment("ServiceTwo.war")
    private URL serviceTwoUrl;

    @Deployment(name = "ServiceOne.war")
    public static Archive<?> deployServiceOne() {
        //Uses the default tracer configuration
        WebArchive serviceOne = ShrinkWrap.create(WebArchive.class, "ServiceOne.war")
                .addClass(TracerIdentityApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return serviceOne;
    }

    @Deployment(name = "ServiceTwo.war")
    public static Archive<?> deployServiceTwo() {
        //Uses the specified tracer configuration
        WebArchive serviceTwo = ShrinkWrap.create(WebArchive.class, "ServiceTwo.war")
                .addClass(TracerIdentityApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml");
        return serviceTwo;
    }

    @Test
    public void testMultipleWarServicesUseDifferentTracers() throws Exception {
        testHttpInvokation();
    }

    @Test
    public void testMultipleWarServicesUseDifferentTracersAfterReload() throws Exception {
        testHttpInvokation();
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
        testHttpInvokation();
    }

    private void testHttpInvokation() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse svcOneResponse = client.execute(new HttpGet(serviceOneUrl.toString() + "service-endpoint/tags"));
            Assert.assertEquals(200, svcOneResponse.getStatusLine().getStatusCode());
            String serviceOneTracer = EntityUtils.toString(svcOneResponse.getEntity());
            Assert.assertEquals("initial", serviceOneTracer);
            HttpResponse svcTwoResponse = client.execute(new HttpGet(serviceTwoUrl.toString() + "service-endpoint/tags"));
            Assert.assertEquals(200, svcTwoResponse.getStatusLine().getStatusCode());
            String serviceTwoTracer = EntityUtils.toString(svcTwoResponse.getEntity());
            Assert.assertNotEquals("Service one and service two tracer instance hash is same - " + serviceTwoTracer,
                    serviceOneTracer, serviceTwoTracer);
            Assert.assertEquals("test", serviceTwoTracer);
        }
    }
}
