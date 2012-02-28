/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.xmldeployment;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

/**
 * Test deployment of -ds.xml files
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(DeployedXmlJMSManagementTestCase.DeployedXmlJMSManagementTestCaseSetup.class)
public class DeployedXmlJMSManagementTestCase {

    public static final String TEST_JMS_XML = "test-jms.xml";

    static class DeployedXmlJMSManagementTestCaseSetup extends AbstractMgmtServerSetupTask {

        @Override
        protected void doSetup(final ManagementClient managementClient) throws Exception {
            final ServerDeploymentManager manager = ServerDeploymentManager.Factory.create(managementClient.getControllerClient());
            final String packageName = DeployedXmlJMSManagementTestCase.class.getPackage().getName().replace(".", "/");
            final DeploymentPlan plan = manager.newDeploymentPlan().add(DeployedXmlJMSManagementTestCase.class.getResource("/" + packageName + "/" + TEST_JMS_XML)).andDeploy().build();
            final Future<ServerDeploymentPlanResult> future = manager.execute(plan);
            final ServerDeploymentPlanResult result = future.get(20, TimeUnit.SECONDS);
            final ServerDeploymentActionResult actionResult = result.getDeploymentActionResult(plan.getId());
            if (actionResult != null) {
                if (actionResult.getDeploymentException() != null) {
                    throw new RuntimeException(actionResult.getDeploymentException());
                }
            }
        }

        @Override
        public void tearDown(final ManagementClient managementClient) throws Exception {
            final ServerDeploymentManager manager = ServerDeploymentManager.Factory.create(managementClient.getControllerClient());
            final DeploymentPlan undeployPlan = manager.newDeploymentPlan().undeploy(TEST_JMS_XML).andRemoveUndeployed().build();
            manager.execute(undeployPlan);
        }
    }

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, "testJMSXmlDeployment.jar")
                .addClass(DeployedXmlJMSManagementTestCase.class)
                .addAsManifestResource(DeployedXmlJMSManagementTestCase.class.getPackage(), "MANIFEST.MF", "MANIFEST.MF");
    }

    @Test
    public void testDeployedQueueInManagementModel() throws IOException {
        final ModelNode address = new ModelNode();
        address.add("deployment", TEST_JMS_XML);
        address.add("subsystem", "messaging");
        address.add("hornetq-server", "default");
        address.add("jms-queue", "queue1");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-attribute");
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set("durable");
        ModelNode result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(true, result.get(RESULT).asBoolean());
    }

    @Test
    public void testDeployedTopicInManagementModel() throws IOException {
        final ModelNode address = new ModelNode();
        address.add("deployment", TEST_JMS_XML);
        address.add("subsystem", "messaging");
        address.add("hornetq-server", "default");
        address.add("jms-topic", "topic1");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-attribute");
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set("entries");
        ModelNode result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals("java:/topic1", result.get(RESULT).asList().get(0).asString());
    }
}
