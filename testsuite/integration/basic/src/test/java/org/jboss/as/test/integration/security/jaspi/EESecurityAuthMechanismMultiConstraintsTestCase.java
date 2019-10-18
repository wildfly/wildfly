/*
 * Copyright 2019 Red Hat, Inc.
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

package org.jboss.as.test.integration.security.jaspi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.jacc.propagation.Manage;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Copeied from {@link EESecurityAuthMechanismTestCase} testing multiple constrained directories.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("MagicNumber")
@RunWith(Arquillian.class)
@ServerSetup({JaspiSecurityDomainsSetup.class})
@RunAsClient
public class EESecurityAuthMechanismMultiConstraintsTestCase {

    @Deployment(name = "WFLY-12655")
    public static WebArchive warDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, EESecurityAuthMechanismMultiConstraintsTestCase.class.getSimpleName() + ".war");

        final StringAsset usersRolesAsset = new StringAsset(Utils.createUsersFromRoles(Manage.ROLES_ALL));
        war.addAsResource(usersRolesAsset, "users.properties")
                .addAsResource(usersRolesAsset, "roles.properties");

        war.addAsWebInfResource(EESecurityAuthMechanismMultiConstraintsTestCase.class.getPackage(), "WFLY-12655-web.xml", "/web.xml")
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(JaspiSecurityDomainsSetup.SECURITY_DOMAIN_NAME), "jboss-web.xml");

        // temporary. remove once the security subsystem is updated to proper consider the module option
        war.addAsManifestResource(Utils.getJBossDeploymentStructure("org.wildfly.extension.undertow"), "jboss-deployment-structure.xml");

        war.add(new StringAsset("Welcome Area"), "area/index.jsp")
                .add(new StringAsset("Welcome Area51"), "area51/index.jsp")
                .add(new StringAsset("Unsecured"), "index.jsp");
        war.addClasses(SimpleHttpAuthenticationMechanism.class, SimpleIdentityStore.class);
        return war;

    }

    @BeforeClass
    public static void skipSecurityManager() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    @Test
    public void testRequiresAuthentication(@ArquillianResource URL webAppURL) throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpResponse httpResponse = httpClient.execute(new HttpGet(webAppURL.toURI() + "/area"));
            assertEquals("Expected /area to require authentication.", 401, httpResponse.getStatusLine().getStatusCode());

            httpResponse = httpClient.execute(new HttpGet(webAppURL.toURI() + "/area51"));
            assertEquals("Expected /area51 to require authentication.", 401, httpResponse.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testSuccessfulAuthentication(@ArquillianResource URL webAppURL) throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

            HttpResponse httpResponse = httpClient.execute(new HttpGet(webAppURL.toURI() + "area/?name=User&pw=User"));
            assertEquals(200, httpResponse.getStatusLine().getStatusCode());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            httpResponse.getEntity().writeTo(bos);
            assertTrue(new String(bos.toByteArray(), StandardCharsets.UTF_8).contains("Welcome Area"));

            httpResponse = httpClient.execute(new HttpGet(webAppURL.toURI() + "area51/?name=User&pw=User"));
            assertEquals(200, httpResponse.getStatusLine().getStatusCode());
            bos = new ByteArrayOutputStream();
            httpResponse.getEntity().writeTo(bos);
            assertTrue(new String(bos.toByteArray(), StandardCharsets.UTF_8).contains("Welcome Area51"));
        }
    }

    @Test
    public void testUnsuccessfulAuthentication(@ArquillianResource URL webAppURL) throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

            HttpResponse httpResponse = httpClient.execute(new HttpGet(webAppURL.toURI() + "area/?name=Invalid&pw=User"));
            assertEquals(401, httpResponse.getStatusLine().getStatusCode());

            httpResponse = httpClient.execute(new HttpGet(webAppURL.toURI() + "area51/?name=Invalid&pw=User"));
            assertEquals(401, httpResponse.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testAuthNotRequired(@ArquillianResource URL webAppURL) throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

            HttpResponse httpResponse = httpClient.execute(new HttpGet(webAppURL.toURI() + "index.jsp"));
            assertEquals(200, httpResponse.getStatusLine().getStatusCode());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            httpResponse.getEntity().writeTo(bos);
            assertTrue(new String(bos.toByteArray(), StandardCharsets.UTF_8).contains("Unsecured"));
        }
    }

}
