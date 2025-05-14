/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.restclient.deployment;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map;
import java.util.Properties;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.Assert;
import org.wildfly.test.integration.microprofile.restclient.deployment.model.Message;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class AbstractDeploymentTest {

    /**
     * Creates a URI builder for the client resource the application.
     *
     * @param deploymentName the deployment name
     *
     * @return a new URI builder for the client resource
     */
    protected UriBuilder uriBuilder(final String deploymentName) {
        return UriBuilder.fromUri(getHttpUri()).path(deploymentName).path("test-app/client");
    }

    /**
     * Reads a message from the client.
     *
     * @param client the client used to issue the get request with
     * @param uri    the URI for the request
     *
     * @return the message response
     */
    Message readMessage(final Client client, final URI uri) {
        try (Response response = client.target(uri).request().get()) {
            Assert.assertEquals(200, response.getStatus());
            final Message message = response.readEntity(Message.class);
            Assert.assertNotNull(message);
            return message;
        }
    }

    /**
     * Adds a {@code microprofile-config.properties} to a deployment with the contents of the map.
     *
     * @param archive the deployment to append the file to
     * @param props   the properties to add
     * @param <T>     the archive type
     *
     * @return the amended archive
     *
     * @throws IOException if an error occurs creating the content
     */
    static <T extends Archive<T>> T addConfigProperties(final T archive, final Map<String, String> props)
            throws IOException {
        try (StringWriter writer = new StringWriter()) {
            final Properties properties = new Properties();
            properties.putAll(props);
            properties.store(writer, "Test MicroProfile REST Client configuration");
            writer.flush();
            return archive.add(new StringAsset(writer.toString()), "META-INF/microprofile-config.properties");
        }
    }

    static URI getHttpUri() {
        return URI.create("http://" + TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort() + "/");
    }
}
