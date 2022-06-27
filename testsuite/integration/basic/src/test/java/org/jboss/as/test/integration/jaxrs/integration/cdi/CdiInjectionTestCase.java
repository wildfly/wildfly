/*
 * Copyright 2022 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.jaxrs.integration.cdi;

import java.net.URISyntaxException;
import java.net.URL;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CdiInjectionTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, CdiInjectionTestCase.class.getSimpleName() + ".war")
                .add(new StringAsset("<beans bean-discovery-mode=\"annotated\"></beans>"), "WEB-INF/beans.xml")
                .addClasses(CDIApplication.class, CDIProvider.class, CDIResource.class, CDIBean.class, InjectionResource.class);
    }


    private static Client CLIENT;

    @ArquillianResource
    private URL url;

    @BeforeClass
    public static void createClient() {
        CLIENT = ClientBuilder.newClient();
    }

    @AfterClass
    public static void closeClient() {
        if (CLIENT != null) {
            CLIENT.close();
        }
    }

    @Test
    public void checkApplication() throws Exception {
        checkResponse("app", CDIApplication.class);
    }

    @Test
    public void checkProvider() throws Exception {
        checkResponse("provider", CDIProvider.class);
    }

    @Test
    public void checkResource() throws Exception {
        checkResponse("resource", CDIResource.class);
    }

    private void checkResponse(final String path, final Class<?> expectedType) throws Exception {
        try (
                Response response = CLIENT.target(createUri(path))
                        .request()
                        .get()
        ) {
            final String text = response.readEntity(String.class);
            Assert.assertEquals(text, Response.Status.OK, response.getStatusInfo());
            Assert.assertTrue(String.format("Expected type %s, but found %s", expectedType.getName(), text), text.startsWith(expectedType.getName()));
        }
    }

    private UriBuilder createUri(final String path) throws URISyntaxException {
        return UriBuilder.fromUri(url.toURI())
                .path("rest/inject/" + path);
    }
}
