/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.setup.SnapshotServerSetupTask;
import org.jboss.as.test.integration.jaxrs.cfg.resources.ErrorResource;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestApplication;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@code resteasy-disable-html-sanitizer} attribute works as expected.
 * <p>
 * Note that order used for the methods is for performance reasons of only having to reload the server once. If a reload
 * becomes not required, the sequence is not important.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@ServerSetup(SnapshotServerSetupTask.class)
public class ResteasyDisableHtmlSanitizerTestCase extends AbstractResteasyAttributeTest {
    private static final String HTML = "<html><body><h1>Error: Failed on purpose</h1></body></html>";
    private static final String ESCAPED_HTML = "&lt;html&gt;&lt;body&gt;&lt;h1&gt;Error: Failed on purpose&lt;&#x2F;h1&gt;&lt;&#x2F;body&gt;&lt;&#x2F;html&gt;";

    @ArquillianResource
    private URI uri;

    public ResteasyDisableHtmlSanitizerTestCase() {
        super("resteasy-disable-html-sanitizer");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyDisableHtmlSanitizerTestCase.class.getSimpleName() + ".war")
                .addClasses(TestApplication.class, ErrorResource.class);
    }

    @Test
    @InSequence(1)
    public void checkDefault() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(uriBuilder()).request().post(Entity.html(HTML))) {
                Assert.assertEquals(400, response.getStatus());
                final String errorHtml = response.readEntity(String.class);
                Assert.assertEquals(ESCAPED_HTML, errorHtml);
            }
        }
    }

    @Test
    @InSequence(2)
    public void checkTrue() throws Exception {
        writeAttribute(ModelNode.TRUE);
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(uriBuilder()).request().post(Entity.html(HTML))) {
                Assert.assertEquals(400, response.getStatus());
                final String errorHtml = response.readEntity(String.class);
                Assert.assertEquals(HTML, errorHtml);
            }
        }
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/error/html");
    }
}
