/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.restclient.deployment;

import java.io.IOException;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.microprofile.restclient.deployment.model.Message;
import org.wildfly.test.integration.microprofile.restclient.deployment.resource.MessageClient;

/**
 * An abstract test for testing multiple MicroProfile Rest Client applications.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class AbstractMultipleDeploymentsTest extends AbstractDeploymentTest {
    static final String DEPLOYMENT_1 = "deployment1";
    static final String DEPLOYMENT_2 = "deployment2";

    /**
     * Creates a WAR with the deployment name and a URL entry for the service name
     *
     * @param deploymentName the name for the deployment, {@code .war} will be appended
     * @param serviceName    the name of the service the client should invocation should happen
     *
     * @return the WAR
     *
     * @throws IOException if an error occurs creating the WAR
     */
    static WebArchive createWar(final String deploymentName, final String serviceName) throws IOException {
        final Map<String, String> config = Map.of(MessageClient.class.getName() + "/mp-rest/uri",
                getHttpUri() + serviceName + "/test-app", "org.wildfly.expansion.test.deployment.name", deploymentName);
        return addConfigProperties(ShrinkWrap.create(WebArchive.class, deploymentName + ".war")
                .addPackage(MessageClient.class.getPackage())
                .addClass(Message.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml"), config);
    }

    /**
     * Sends a message to deployment1 which uses a client to send a message to deployment2. We then query the
     * response to ensure we get the expected message. The source of the message should be deployment2 as that was the
     * final receiver of the message.
     */
    @Test
    @InSequence(1)
    public void sendDeployment1Message() {
        try (Client client = ClientBuilder.newClient()) {
            final Message message = new Message();
            message.setText("Hello World");
            try (
                    Response response = client.target(uriBuilder(DEPLOYMENT_1))
                            .request()
                            .post(Entity.json(message))
            ) {
                Assert.assertEquals(201, response.getStatus());
                final Message foundMessage = readMessage(client, response.getLocation());
                Assert.assertEquals("Hello World", foundMessage.getText());
                Assert.assertEquals("Expected the request to be sent to " + DEPLOYMENT_2, DEPLOYMENT_2, foundMessage.getTarget());
            }
        }
    }

    /**
     * Through the client on deployment1, we query the first message on deployment2.
     */
    @Test
    @InSequence(2)
    public void getDeployment1Message() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder(DEPLOYMENT_1).path("1"))
                            .request()
                            .get()
            ) {
                Assert.assertEquals(200, response.getStatus());
                final Message foundMessage = response.readEntity(Message.class);
                Assert.assertEquals("Hello World", foundMessage.getText());
                Assert.assertEquals("Expected the request to be sent to " + DEPLOYMENT_2, DEPLOYMENT_2, foundMessage.getTarget());
            }
        }
    }

    /**
     * Through the client on deployment1, we delete the message on deployment2.
     */
    @Test
    @InSequence(3)
    public void deleteDeployment1Message() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder(DEPLOYMENT_1).path("1"))
                            .request()
                            .delete()
            ) {
                Assert.assertEquals(200, response.getStatus());
                final Message foundMessage = response.readEntity(Message.class);
                Assert.assertEquals("Hello World", foundMessage.getText());
                Assert.assertEquals("Expected the request to be sent to " + DEPLOYMENT_2, DEPLOYMENT_2, foundMessage.getTarget());
            }
        }
    }

    /**
     * Sends a message to deployment2 which uses a client to send a message to deployment1. We then query the
     * response to ensure we get the expected message. The source of the message should be deployment1 as that was the
     * final receiver of the message.
     */
    @Test
    @InSequence(4)
    public void sendDeployment2Message() {
        try (Client client = ClientBuilder.newClient()) {
            final Message message = new Message();
            message.setText("Hello World");
            try (
                    Response response = client.target(uriBuilder(DEPLOYMENT_2))
                            .request()
                            .post(Entity.json(message))
            ) {
                Assert.assertEquals(201, response.getStatus());
                final Message foundMessage = readMessage(client, response.getLocation());
                Assert.assertEquals("Hello World", foundMessage.getText());
                Assert.assertEquals("Expected the request to be sent to " + DEPLOYMENT_1, DEPLOYMENT_1, foundMessage.getTarget());
            }
        }
    }

    /**
     * Through the client on deployment2, we query the first message on deployment1.
     */
    @Test
    @InSequence(4)
    public void getDeployment2Message() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder(DEPLOYMENT_2).path("1"))
                            .request()
                            .get()
            ) {
                Assert.assertEquals(200, response.getStatus());
                final Message foundMessage = response.readEntity(Message.class);
                Assert.assertEquals("Hello World", foundMessage.getText());
                Assert.assertEquals("Expected the request to be sent to " + DEPLOYMENT_1, DEPLOYMENT_1, foundMessage.getTarget());
            }
        }
    }

    /**
     * Through the client on deployment2, we delete the message on deployment1.
     */
    @Test
    @InSequence(5)
    public void deleteDeployment2Message() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder(DEPLOYMENT_2).path("1"))
                            .request()
                            .delete()
            ) {
                Assert.assertEquals(200, response.getStatus());
                final Message foundMessage = response.readEntity(Message.class);
                Assert.assertEquals("Hello World", foundMessage.getText());
                Assert.assertEquals("Expected the request to be sent to " + DEPLOYMENT_1, DEPLOYMENT_1, foundMessage.getTarget());
            }
        }
    }
}
