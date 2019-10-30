/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.jms.deployment;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that invoking a management operation that removes a JMS resource that is used by a deployed archive must fail:
 * the resource must not be removed and any depending services must be recovered.
 * The deployment must still be operating after the failing management operation.

 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(DependentMessagingDeploymentTestCase.MessagingResourcesSetupTask.class)
public class DependentMessagingDeploymentTestCase {

    public static final String QUEUE_LOOKUP = "java:/jms/DependentMessagingDeploymentTestCase/myQueue";
    public static final String TOPIC_LOOKUP = "java:/jms/DependentMessagingDeploymentTestCase/myTopic";

    private static final String QUEUE_NAME = "myQueue";
    private static final String TOPIC_NAME = "myTopic";

    @ContainerResource
    private ManagementClient managementClient;

    @ArquillianResource
    private URL url;

    static class MessagingResourcesSetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            JMSOperationsProvider.getInstance(managementClient).createJmsQueue(QUEUE_NAME, QUEUE_LOOKUP);
            JMSOperationsProvider.getInstance(managementClient).createJmsTopic(TOPIC_NAME, TOPIC_LOOKUP);
        }


        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            JMSOperationsProvider.getInstance(managementClient).removeJmsQueue(QUEUE_NAME);
            JMSOperationsProvider.getInstance(managementClient).removeJmsTopic(TOPIC_NAME);
        }
    }

    @Deployment
    public static WebArchive createArchive() {
        return create(WebArchive.class, "DependentMessagingDeploymentTestCase.war")
                .addClass(MessagingServlet.class)
                .addClasses(QueueMDB.class, TopicMDB.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testRemoveDependingQueue() throws Exception {
        sendAndReceiveMessage(true);

        try {
            JMSOperationsProvider.getInstance(managementClient).removeJmsQueue(QUEUE_NAME);
            fail("removing a JMS resource that the deployment is depending upon must fail");
        } catch(Exception e) {
        }

        sendAndReceiveMessage(true);
    }

    @Test
    public void testRemoveDependingTopic() throws Exception {
        sendAndReceiveMessage(false);

        try {
            JMSOperationsProvider.getInstance(managementClient).removeJmsTopic(TOPIC_NAME);
            fail("removing a JMS resource that the deployment is depending upon must fail");
        } catch(Exception e) {
        }

        sendAndReceiveMessage(false);
    }

    private void sendAndReceiveMessage(boolean sendToQueue) throws Exception {
        String destination = sendToQueue ? "queue" : "topic";
        String text = UUID.randomUUID().toString();
        URL url = new URL(this.url.toExternalForm() + "DependentMessagingDeploymentTestCase?destination=" + destination + "&text=" + text);
        String reply = HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);

        assertNotNull(reply);
        assertEquals(text, reply);
    }
}
