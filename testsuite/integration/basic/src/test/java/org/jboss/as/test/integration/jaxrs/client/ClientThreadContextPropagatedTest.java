/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.client;

import java.net.URL;
import java.util.Collection;
import java.util.PropertyPermission;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class ClientThreadContextPropagatedTest {

    @ArquillianResource
    private URL url;

    private Client client;

    @BeforeEach
    public void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, ClientThreadContextPropagatedTest.class.getSimpleName() + ".war")
                .addClasses(RestActivator.class, ClientThreadContextResource.class, TimeoutUtil.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                        new PropertyPermission("ts.timeout.factor", "read"))
                , "permissions.xml");
    }

    @Test
    public void async() throws Exception {
        final Future<String> future = client.target(url + "context/async")
                .request(MediaType.TEXT_PLAIN_TYPE)
                .async()
                .get(String.class);
        final String value = future.get(2, TimeUnit.SECONDS);
        Assertions.assertEquals("/context/async", value);
    }

    @Test
    public void delayed() throws Exception {
        final Future<String> future = client.target(url + "context/async/delayed")
                .request(MediaType.TEXT_PLAIN_TYPE)
                .async()
                .get(String.class);
        final String value = future.get(2, TimeUnit.SECONDS);
        Assertions.assertEquals("/context/async/delayed", value);
    }

    @Test
    public void managedThreadFactory() throws Exception {
        final Future<String> future = client.target(url + "context/async/thread-factory")
                .request(MediaType.TEXT_PLAIN_TYPE)
                .async()
                .get(String.class);
        final String value = future.get(2, TimeUnit.SECONDS);
        Assertions.assertEquals("/context/async/thread-factory", value);
    }

    @Test
    public void scheduled() throws Exception {
        @SuppressWarnings("unchecked")
        final Collection<String> results = client.target(url + "context/async/scheduled/3")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(Collection.class);
        // We should end up with 3 results
        Assertions.assertEquals(3, results.size(), String.format("Expected 3 entries found: %d - %s", results.size(), results));
        // Compare the results
        int index = 0;
        for (String found : results) {
            Assertions.assertEquals("/context/async/scheduled/3-" + index++, found);
        }
    }
}
