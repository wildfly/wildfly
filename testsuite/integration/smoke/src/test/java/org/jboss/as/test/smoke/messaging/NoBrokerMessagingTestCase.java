/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.messaging;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple test to cover the deployment of a simple web application whithout any internal broker.
 * Checks that without the default Jakarta Messaging factory all is working properly.
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(NoBrokerMessagingTestCase.SetupTask.class)
public class NoBrokerMessagingTestCase {

    @ArquillianResource
    private URL url;

    static class SetupTask extends SnapshotRestoreSetupTask {

        private static final Logger logger = Logger.getLogger(NoBrokerMessagingTestCase.SetupTask.class);

        @Override
        public void doSetup(org.jboss.as.arquillian.container.ManagementClient managementClient, String s) throws Exception {
            execute(managementClient, Operations.createUndefineAttributeOperation(PathAddress.parseCLIStyleAddress("/subsystem=ee/service=default-bindings").toModelNode(), "jms-connection-factory"), true);
            execute(managementClient, Operations.createRemoveOperation(PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default").toModelNode()), true);
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        private ModelNode execute(final org.jboss.as.arquillian.container.ManagementClient managementClient, final ModelNode op, final boolean expectSuccess) throws IOException {
            ModelNode response = managementClient.getControllerClient().execute(op);
            final String outcome = response.get("outcome").asString();
            if (expectSuccess) {
                assertEquals(response.toString(), "success", outcome);
                return response.get("result");
            } else {
                assertEquals("failed", outcome);
                return response.get("failure-description");
            }
        }
    }

    @Deployment
    public static WebArchive createArchive() {
        return create(WebArchive.class, "nobroker.war")
                .addAsWebResource(new StringAsset("<!DOCTYPE html>\n"
                        + "<html lang= \"en\">\n"
                        + "  <head>\n"
                        + "    <meta charset=\"utf-8\">\n"
                        + "    <title>No Broker</title>\n"
                        + "  </head>\n"
                        + "  <body>\n"
                        + "    <h1>Simple Content test for nobroker.war</h1>\n"
                        + "    <p>This is not a 404 error.</p>\n"
                        + "  </body>\n"
                        + "</html>"), "index.html");
    }

    @Test
    public void testWarIsDeployed() throws Exception {
        String reply = HttpRequest.get(this.url.toExternalForm(), TimeoutUtil.adjust(10), TimeUnit.SECONDS);
        assertNotNull(reply);
    }
}
