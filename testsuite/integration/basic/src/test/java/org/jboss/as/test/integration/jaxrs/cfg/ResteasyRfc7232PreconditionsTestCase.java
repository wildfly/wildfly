/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.jaxrs.cfg.resources.PreconditionRfc7232PrecedenceResource;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestApplication;
import org.jboss.as.test.shared.ExtendedSnapshotServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@code resteasy-rfc7232preconditions} attribute works as expected.
 * <p>
 * Note that order used for the methods is for performance reasons of only having to reload the server once. If a reload
 * becomes not required, the sequence is not important.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@ServerSetup(ExtendedSnapshotServerSetupTask.class)
public class ResteasyRfc7232PreconditionsTestCase extends AbstractResteasyAttributeTest {

    @ArquillianResource
    private URI uri;

    public ResteasyRfc7232PreconditionsTestCase() {
        super("resteasy-rfc7232preconditions");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyRfc7232PreconditionsTestCase.class.getSimpleName() + ".war")
                .addClasses(
                        TestApplication.class,
                        PreconditionRfc7232PrecedenceResource.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @InSequence(1)
    public void checkDefault() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .header(HttpHeaders.IF_MATCH, "1")
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, "Mon, 1 Jan 2007 00:00:00 GMT")
                            .header(HttpHeaders.IF_NONE_MATCH, "2")
                            .header(HttpHeaders.IF_MODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT")
                            .get()
            ) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("preconditions met", response.readEntity(String.class));
            }
        }
    }

    @Test
    @InSequence(2)
    public void checkDefaultFail() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .header(HttpHeaders.IF_NONE_MATCH, "2")
                            .header(HttpHeaders.IF_MODIFIED_SINCE, "Mon, 1 Jan 2007 00:00:00 GMT")
                            .get()
            ) {
                // Preconditions failed with a 304 as the content has not been modified. Note this is testing that
                // without the resteasy-rfc7232preconditions that the If-None-Match is not ignored. See
                // noneMatchIgnoreIfModifiedSince for an expected passing version.
                Assert.assertEquals(304, response.getStatus());
            }
        }
    }

    @Test
    @InSequence(4)
    public void checkTrue() throws Exception {
        writeAttribute(ModelNode.TRUE);
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .header(HttpHeaders.IF_MATCH, "1")
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, "Mon, 1 Jan 2007 00:00:00 GMT")
                            .header(HttpHeaders.IF_NONE_MATCH, "2")
                            .header(HttpHeaders.IF_MODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT")
                            .get()
            ) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("preconditions met", response.readEntity(String.class));
            }
        }
    }

    @Test
    @InSequence(5)
    public void noneMatchIgnoreIfModifiedSince() throws Exception {
        writeAttribute(ModelNode.TRUE);
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .header(HttpHeaders.IF_NONE_MATCH, "2")
                            .header(HttpHeaders.IF_MODIFIED_SINCE, "Mon, 1 Jan 2007 00:00:00 GMT")
                            .get()
            ) {
                // Per RFC 7232:
                // A recipient must ignore If-Modified-Since if the request contains an If-None-Match header field;
                // the condition in If-None-Match is considered to be a more accurate replacement for the condition in
                // If-Modified-Since, and the two are only combined for the sake of interoperating with older
                // intermediaries that might not implement If-None-Match.
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("preconditions met", response.readEntity(String.class));
            }
        }
    }

    @Test
    @InSequence(6)
    public void matchIgnoreIfModifiedSince() throws Exception {
        writeAttribute(ModelNode.TRUE);
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .header(HttpHeaders.IF_MATCH, "1")
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, "Mon, 2 Jan 2007 00:00:00 GMT")
                            .get()
            ) {
                // Per RFC 7232:
                // A recipient MUST ignore If-Unmodified-Since if the request contains an If-Match header field; the
                // condition in If-Match is considered to be a more accurate replacement for the condition in
                // If-Unmodified-Since, and the two are only combined for the sake of interoperating with older
                // intermediaries that might not implement If-Match.
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("preconditions met", response.readEntity(String.class));
            }
        }
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/precedence");
    }
}
