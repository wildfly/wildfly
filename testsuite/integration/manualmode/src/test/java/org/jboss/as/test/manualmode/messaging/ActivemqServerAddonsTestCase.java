/*
 * Copyright 2022 JBoss by Red Hat.
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
package org.jboss.as.test.manualmode.messaging;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.PropertyPermission;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.manualmode.messaging.deployment.MessagingServlet;
import org.jboss.as.test.manualmode.messaging.deployment.QueueMDB;
import org.jboss.as.test.manualmode.messaging.deployment.TopicMDB;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ServerControl;

/**
 *
 * @author Emmanuel Hugonnet (c) 2022 Red Hat, Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
@ServerControl(manual = true)
public class ActivemqServerAddonsTestCase {

    private static final String DEFAULT_FULL_JBOSSAS = "default-full-jbossas";

    @ArquillianResource
    protected static ContainerController container;
    @ArquillianResource
    private Deployer deployer;

    private final TestModule module = new TestModule("org.apache.activemq.artemis.addons", "org.apache.activemq.artemis");

    private static final String LB_CLASS_ATT = "connection-load-balancing-policy-class-name";
    private static final String QUEUE_LOOKUP = "java:/jms/DependentMessagingDeploymentTestCase/myQueue";
    private static final String TOPIC_LOOKUP = "java:/jms/DependentMessagingDeploymentTestCase/myTopic";

    private static final String QUEUE_NAME = "myQueue";
    private static final String TOPIC_NAME = "myTopic";
    private static final String DEPLOYMENT = "addons-deployment";

    @Before
    public void setup() throws Exception {
        try ( ManagementClient managementClient = createManagementClient()) {
            if (container.isStarted(DEFAULT_FULL_JBOSSAS)) {
                container.stop(DEFAULT_FULL_JBOSSAS);
            }
            final JavaArchive jar = module.addResource("artemis-lb-policy.jar");
            jar.addClasses(OrderedLoadBalancingPolicy.class);
            System.setProperty("module.path", Paths.get("target" ,"wildfly", "modules").toAbsolutePath().toString());
            module.create(true);
            if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
                container.start(DEFAULT_FULL_JBOSSAS);
            }
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("server", "default");
            address.add("pooled-connection-factory", "activemq-ra");
            ModelNode op = Operations.createWriteAttributeOperation(address, LB_CLASS_ATT, OrderedLoadBalancingPolicy.class.getName());
            execute(managementClient, op, true);
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsOperations.createJmsQueue(QUEUE_NAME, QUEUE_LOOKUP);
            jmsOperations.createJmsTopic(TOPIC_NAME, TOPIC_LOOKUP);
            jmsOperations.close();
            deployer.deploy(DEPLOYMENT);
        }
    }

    private ModelNode execute(final org.jboss.as.arquillian.container.ManagementClient managementClient, final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final boolean success = Operations.isSuccessfulOutcome(response);
        if (expectSuccess) {
            assertTrue(response.toString(), success);
            return Operations.readResult(response);
        } else {
            assertFalse(response.toString(), success);
            return Operations.getFailureDescription(response);
        }
    }

    @After
    public void tearDown() throws Exception {
        deployer.undeploy(DEPLOYMENT);
        try ( ManagementClient managementClient = createManagementClient()) {
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsOperations.removeJmsQueue(QUEUE_NAME);
            jmsOperations.removeJmsTopic(TOPIC_NAME);
            jmsOperations.close();
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("server", "default");
            address.add("pooled-connection-factory", "activemq-ra");
            ModelNode op = Operations.createUndefineAttributeOperation(address, LB_CLASS_ATT);
            execute(managementClient, op, true);
        }
        container.stop(DEFAULT_FULL_JBOSSAS);
        module.remove();
        System.clearProperty("module.path");
    }

    private static ManagementClient createManagementClient() throws UnknownHostException {
        return new ManagementClient(
                TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.formatPossibleIpv6Address(TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort(),
                "remote+http");
    }

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(DEFAULT_FULL_JBOSSAS)
    public static WebArchive createArchive() {
        return create(WebArchive.class, DEPLOYMENT +".war")
                .addClasses(MessagingServlet.class, TimeoutUtil.class)
                .addClasses(QueueMDB.class, TopicMDB.class)
                .addAsWebInfResource(new StringAsset("<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                        + "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\"\n"
                        + "bean-discovery-mode=\"all\">\n"
                        + "</beans>"), "beans.xml")
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");
    }

    @Test
    public void testQueue() throws Exception {
        String destination = "queue";
        String text = UUID.randomUUID().toString();
        URL servletUrl = new URL(TestSuiteEnvironment.getHttpUrl().toExternalForm() + '/' + DEPLOYMENT + "/DependentMessagingDeploymentTestCase?destination=" + destination + "&text=" + text);
        String reply = HttpRequest.get(servletUrl.toExternalForm(), 10, TimeUnit.SECONDS);
        assertNotNull(reply);
        assertEquals(text, reply);
    }

}
