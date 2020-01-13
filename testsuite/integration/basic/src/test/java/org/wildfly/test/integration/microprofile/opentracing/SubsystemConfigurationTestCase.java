/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.integration.microprofile.opentracing;

import static org.jboss.as.test.shared.ServerReload.executeReloadAndWaitForCompletion;

import io.opentracing.noop.NoopTracer;
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
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.opentracing.application.TracerConfigurationApplication;

/**
 * Tracers in user applications should be able to use Jaeger tracer configuration defined by WildFly Management Model.
 *  If no tracer is defined for deployment (no default tracer and 'smallrye.opentracing.serviceName' is set to an empty string
 * then a NoopTracer should be used.
 *
 * @author Sultan Zhantemirov (c) 2019 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SubsystemConfigurationTestCase.SetupTask.class)
public class SubsystemConfigurationTestCase {

    private static final String WEB_XML
            = "<web-app version=\"3.1\" xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n"
            + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "        xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd\">\n"
            + "    <context-param>\n"
            + "        <param-name>mp.opentracing.extensions.tracer.configuration</param-name>\n"
            + "        <param-value>jaeger-test</param-value>\n"
            + "    </context-param>\n"
            + "</web-app>";

    // smallrye.opentracing.serviceName is set to an empty string
    // in order to use NoopTracer when no default tracer configuration is defined
    private static final String WEB_XML_NOOP
            = "<web-app version=\"3.1\" xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n"
            + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "        xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd\">\n"
            + "    <context-param>\n"
            + "        <!-- Setting smallrye.opentracing.serviceName to an empty string so a NoopTracer is used -->"
            + "        <param-name>smallrye.opentracing.serviceName</param-name>\n"
            + "        <param-value></param-value>\n"
            + "    </context-param>\n"
            + "</web-app>";



    @ArquillianResource
    @OperateOnDeployment("ServiceCheck.war")
    private URL serviceCheckUrl;

    @ArquillianResource
    @OperateOnDeployment("ServiceNoop.war")
    private URL serviceNoopUrl;

    @Deployment(name = "ServiceCheck.war")
    public static Archive<?> deployCheck() {
        return ShrinkWrap.create(WebArchive.class, "tracerConfigurationCheck.war")
                .addClass(TracerConfigurationApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml");
    }

    @Deployment(name = "ServiceNoop.war")
    public static Archive<?> deployNoop() {
        return ShrinkWrap.create(WebArchive.class, "noopTracerConfigurationCheck.war")
                .addClass(TracerConfigurationApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(new StringAsset(WEB_XML_NOOP), "web.xml");
    }

    @Test
    @OperateOnDeployment("ServiceCheck.war")
    public void checkJaegerTracerAttributes() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(serviceCheckUrl.toString() + "tracer-config/get"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String tracerConfiguration = EntityUtils.toString(response.getEntity());

            Assert.assertTrue(tracerConfiguration.contains("sampler.type=probabilistic"));
            Assert.assertTrue(tracerConfiguration.contains("sampler.param=0.5"));
            Assert.assertTrue(tracerConfiguration.contains("useTraceId128Bit=true"));
        }
    }

    @Test
    @OperateOnDeployment("ServiceNoop.war")
    public void checkNoopTracerConfiguration() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(serviceNoopUrl.toString() + "tracer-config/get"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String tracerName = EntityUtils.toString(response.getEntity());

            Assert.assertEquals("NoopTracer configuration is not used", NoopTracer.class.getSimpleName(), tracerName);
        }
    }

    public static class SetupTask implements ServerSetupTask {

        private static final PathAddress OT_SUBSYSTEM = PathAddress.parseCLIStyleAddress("/subsystem=microprofile-opentracing-smallrye");

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode addTracer = Operations.createAddOperation(OT_SUBSYSTEM.append("jaeger-tracer", "jaeger-test").toModelNode());
            addTracer.get("sampler-type").set("probabilistic");
            addTracer.get("sampler-param").set(0.5D);
            addTracer.get("tracer_id_128bit").set(true);
            Utils.applyUpdate(addTracer, managementClient.getControllerClient());
            // undefine default tracer configuration in order to get NoopTracer configuration
            Utils.applyUpdate(Operations.createUndefineAttributeOperation(OT_SUBSYSTEM.toModelNode(), ("default-tracer")), managementClient.getControllerClient());
            executeReloadAndWaitForCompletion(managementClient, TimeoutUtil.adjust(100000));
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            // recover default tracer configuration
            Utils.applyUpdate(Operations.createWriteAttributeOperation(OT_SUBSYSTEM.toModelNode(), "default-tracer", "jaeger"), managementClient.getControllerClient());
            Utils.applyUpdate(Operations.createRemoveOperation(OT_SUBSYSTEM.append("jaeger-tracer", "jaeger-test").toModelNode()), managementClient.getControllerClient());
            executeReloadAndWaitForCompletion(managementClient, TimeoutUtil.adjust(100000));
        }
    }
}
