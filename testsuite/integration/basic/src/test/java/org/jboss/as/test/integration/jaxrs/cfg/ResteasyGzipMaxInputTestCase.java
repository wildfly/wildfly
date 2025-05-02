/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.Variant;
import jakarta.ws.rs.ext.Providers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.jaxrs.cfg.resources.EchoResource;
import org.jboss.as.test.integration.jaxrs.cfg.resources.SimpleText;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestApplication;
import org.jboss.as.test.shared.ExtendedSnapshotServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.resteasy.plugins.interceptors.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.GZIPEncodingInterceptor;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@code resteasy-gzip-max-input} attribute works as expected.
 * <p>
 * Note that order used for the methods is for performance reasons of only having to reload the server once. If a reload
 * becomes not required, the sequence is not important.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@ServerSetup(ExtendedSnapshotServerSetupTask.class)
public class ResteasyGzipMaxInputTestCase extends AbstractResteasyAttributeTest {

    private static final String PAYLOAD = ("This is a test payload for " + ResteasyGzipMaxInputTestCase.class.getName());

    private final Variant variant = new Variant(MediaType.TEXT_PLAIN_TYPE, "", "gzip");

    @ArquillianResource
    private URI uri;

    public ResteasyGzipMaxInputTestCase() {
        super("resteasy-gzip-max-input");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyGzipMaxInputTestCase.class.getSimpleName() + ".war")
                .addClasses(TestApplication.class, EchoResource.class, SimpleText.class)
                // Enable gzip support in RESTEasy
                .addAsServiceProviderAndClasses(Providers.class,
                        GZIPDecodingInterceptor.class,
                        GZIPEncodingInterceptor.class,
                        AcceptEncodingGZIPFilter.class);
    }

    @Test
    @InSequence(1)
    public void checkDefault() {
        try (Client client = createClient()) {
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .post(Entity.entity(PAYLOAD, variant))
            ) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals(PAYLOAD, response.readEntity(String.class));
            }
        }
    }

    @Test
    @InSequence(2)
    public void checkFailure() throws Exception {
        writeAttribute(new ModelNode(PAYLOAD.length() - 10));
        try (Client client = createClient()) {
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .post(Entity.entity(PAYLOAD, variant))
            ) {
                Assert.assertEquals(413, response.getStatus());
            }
        }
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/echo/gzip");
    }

    private static Client createClient() {
        return ClientBuilder.newBuilder()
                .register(GZIPEncodingInterceptor.class)
                .register(AcceptEncodingGZIPFilter.class)
                .register(GZIPDecodingInterceptor.class)
                .build();
    }
}
