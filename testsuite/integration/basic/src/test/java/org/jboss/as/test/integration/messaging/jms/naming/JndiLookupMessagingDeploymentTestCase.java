/*
 * Copyright 2018 JBoss by Red Hat.
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

package org.jboss.as.test.integration.messaging.jms.naming;



import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple test to cover https://issues.jboss.org/browse/WFLY-9305
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(JndiLookupMessagingDeploymentTestCase.SetupTask.class)
public class JndiLookupMessagingDeploymentTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive createArchive() {
        return create(WebArchive.class, "JndiMessagingDeploymentTestCase.war")
                .addClass(MessagingServlet.class)
                .addClasses(JMSSender.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testSendMessageInClientQueue() throws Exception {
        sendAndReceiveMessage(true);
    }

    private void sendAndReceiveMessage(boolean sendToQueue) throws Exception {
        String text = "test_" + UUID.randomUUID().toString();
        URL url = new URL(this.url.toExternalForm() + "JndiMessagingDeploymentTestCase?text=" + text);
        String reply = HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);

        assertNotNull(reply);
        assertEquals(text, reply);
    }

    static class SetupTask implements ServerSetupTask{

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            managementClient.getControllerClient().execute(Operations.createWriteAttributeOperation(new ModelNode().add("subsystem", "ee"), "annotation-property-replacement", true));
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            managementClient.getControllerClient().execute(Operations.createWriteAttributeOperation(new ModelNode().add("subsystem", "ee"), "annotation-property-replacement", ops.isRemoteBroker()));
        }

    }
}
