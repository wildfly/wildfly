/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.jaxrs.client;

import java.net.URL;
import java.util.Collection;
import java.util.PropertyPermission;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClientThreadContextPropagatedTest {

    @ArquillianResource
    private URL url;

    private Client client;

    @Before
    public void setup() {
        client = ClientBuilder.newClient();
    }

    @After
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
        Assert.assertEquals("/context/async", value);
    }

    @Test
    public void delayed() throws Exception {
        final Future<String> future = client.target(url + "context/async/delayed")
                .request(MediaType.TEXT_PLAIN_TYPE)
                .async()
                .get(String.class);
        final String value = future.get(2, TimeUnit.SECONDS);
        Assert.assertEquals("/context/async/delayed", value);
    }

    @Test
    public void managedThreadFactory() throws Exception {
        final Future<String> future = client.target(url + "context/async/thread-factory")
                .request(MediaType.TEXT_PLAIN_TYPE)
                .async()
                .get(String.class);
        final String value = future.get(2, TimeUnit.SECONDS);
        Assert.assertEquals("/context/async/thread-factory", value);
    }

    @Test
    public void scheduled() throws Exception {
        @SuppressWarnings("unchecked")
        final Collection<String> results = client.target(url + "context/async/scheduled/3")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(Collection.class);
        // We should end up with 3 results
        Assert.assertEquals(String.format("Expected 3 entries found: %d - %s", results.size(), results), 3, results.size());
        // Compare the results
        int index = 0;
        for (String found : results) {
            Assert.assertEquals("/context/async/scheduled/3-" + index++, found);
        }
    }
}
