/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.localtransaction;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
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
 * Tests the behaviour of allowing local transactions for a Jakarta Messaging session from a Servlet.
 *
 * Default behaviour is to disallow it.
 * It can be overridden by specifying allow-local-transactions=true on the pooled-connection-factory resource.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(LocalTransactionTestCase.SetupTask.class)
public class LocalTransactionTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive createArchive() {
        return create(WebArchive.class, "LocalTransactionTestCase.war")
                .addClass(MessagingServlet.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testAllowLocalTransactions() throws Exception {
        callServlet(true);
    }

    @Test
    public void testDisallowLocalTransactions() throws Exception {
        callServlet(false);
    }

    private void callServlet(boolean allowLocalTransactions) throws Exception {
        URL url = new URL(this.url.toExternalForm() + "LocalTransactionTestCase?allowLocalTransactions=" + allowLocalTransactions);
        String reply  = HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
        assertNotNull(reply);
        assertEquals(allowLocalTransactions, Boolean.valueOf(reply));
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
