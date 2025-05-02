/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestApplication;
import org.jboss.as.test.integration.jaxrs.cfg.resources.WiderMappingDefaultOptions;
import org.jboss.as.test.integration.jaxrs.cfg.resources.WiderMappingResource;
import org.jboss.as.test.shared.ExtendedSnapshotServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@code resteasy-wider-request-matching} attribute works as expected.
 * <p>
 * Note that order used for the methods is for performance reasons of only having to reload the server once. If a reload
 * becomes not required, the sequence is not important.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@ServerSetup(ExtendedSnapshotServerSetupTask.class)
public class ResteasyWiderRequestMatchingTestCase extends AbstractResteasyAttributeTest {

    @ArquillianResource
    private URI uri;

    public ResteasyWiderRequestMatchingTestCase() {
        super("resteasy-wider-request-matching");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyWiderRequestMatchingTestCase.class.getSimpleName() + ".war")
                .addClasses(
                        TestApplication.class,
                        WiderMappingResource.class,
                        WiderMappingDefaultOptions.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @InSequence(1)
    public void checkDefault() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .options()
            ) {
                Assert.assertEquals(200, response.getStatus());
                // This invokes the OPTION HTTP method in the WiderMappingResource.post()
                Assert.assertEquals("POST, OPTIONS", response.readEntity(String.class));
            }
        }
    }

    @Test
    @InSequence(2)
    public void checkUnregisteredProvider() throws Exception {
        writeAttribute(ModelNode.TRUE);

        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .options()
            ) {
                // This invokes the OPTION HTTP method in WiderMappingDefaultOptions.options()
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("hello", response.readEntity(String.class));
            }
        }
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/wider/string");
    }
}
