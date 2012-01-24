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
import java.net.InetAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.as.test.integration.deployment.xml.datasource.DeployedXmlDataSourceTestCase;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;

/**
 * Test deployment of -jms.xml files
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class DeployedXmlJMSTestCase {


    private static ServerDeploymentManager manager;
    public static final String TEST_JMS_XML = "test-jms.xml";

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, "testDsXmlDeployment.jar")
                .addPackage(DeployedXmlDataSourceTestCase.class.getPackage())
                .addAsManifestResource(DeployedXmlDataSourceTestCase.class.getPackage(), "MANIFEST.MF", "MANIFEST.MF");
    }

    @ArquillianResource
    private InitialContext initialContext;

    private static ModelControllerClient client;

    @BeforeClass
    public static void deployJms() throws Throwable {
        try {
            client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());
            manager = ServerDeploymentManager.Factory.create(client);
            final String packageName = DeployedXmlJMSTestCase.class.getPackage().getName().replace(".", "/");
            final DeploymentPlan plan = manager.newDeploymentPlan().add(DeployedXmlJMSTestCase.class.getResource("/" + packageName + "/" + TEST_JMS_XML)).andDeploy().build();
            final Future<ServerDeploymentPlanResult> future = manager.execute(plan);
            final ServerDeploymentPlanResult result = future.get(20, TimeUnit.SECONDS);
            final ServerDeploymentActionResult actionResult = result.getDeploymentActionResult(plan.getId());
            if (actionResult != null) {
                if (actionResult.getDeploymentException() != null) {
                    throw actionResult.getDeploymentException();
                }
            }
        } catch (Throwable e) {
            if (client != null) {
                client.close();
            }
        }
    }

    @AfterClass
    public static void undeployJms() throws IOException {
        if (client != null) {
            final DeploymentPlan undeployPlan = manager.newDeploymentPlan().undeploy(TEST_JMS_XML).andRemoveUndeployed().build();
            manager.execute(undeployPlan);
            client.close();
            client = null;
            manager = null;
        }
    }

    @Test
    public void testDeployedQueue() throws Throwable {
        final Queue queue = (Queue) initialContext.lookup("java:/queue1");
        Assert.assertNotNull(queue);
    }

    @Test
    public void testDeployedTopic() throws Throwable {
        final Topic topic = (Topic) initialContext.lookup("java:/topic1");
        Assert.assertNotNull(topic);
    }


}
