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

package org.wildfly.test.integration.elytron.securityapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.wildfly.test.integration.elytron.securityapi.TestAuthenticationMechanism.PASSWORD_HEADER;
import static org.wildfly.test.integration.elytron.securityapi.TestAuthenticationMechanism.USERNAME_HEADER;
import static org.wildfly.test.integration.elytron.securityapi.TestIdentityStore.PASSWORD;
import static org.wildfly.test.integration.elytron.securityapi.TestIdentityStore.USERNAME;

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
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * Copied from {@link EESecurityAuthMechanismTestCase} testing multiple constrained directories.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("MagicNumber")
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ EESecurityAuthMechanismMultiConstraintsTestCase.ServerSetup.class })
public class EESecurityAuthMechanismMultiConstraintsTestCase {

    @Deployment(name = "WFLY-12655")
    public static WebArchive warDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, EESecurityAuthMechanismMultiConstraintsTestCase.class.getSimpleName() + ".war");

        war
            .addAsWebInfResource(EESecurityAuthMechanismMultiConstraintsTestCase.class.getPackage(), "WFLY-12655-web.xml", "/web.xml")
            .addAsWebInfResource(Utils.getJBossWebXmlAsset("SecurityAPI"), "jboss-web.xml");

        war.add(new StringAsset("Welcome Area"), "area/index.jsp")
                .add(new StringAsset("Welcome Area51"), "area51/index.jsp")
                .add(new StringAsset("Unsecured"), "index.jsp");

        war
            .addClasses(EESecurityAuthMechanismMultiConstraintsTestCase.class, AbstractElytronSetupTask.class, ServerSetup.class)
            .addClasses(TestAuthenticationMechanism.class, TestIdentityStore.class);

        return war;

    }

    @BeforeClass
    public static void skipSecurityManager() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    @Test
    public void testRequiresAuthentication(@ArquillianResource URL webAppURL) throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet request = new HttpGet(webAppURL.toURI() + "/area");
            HttpResponse httpResponse = httpClient.execute(request);
            assertEquals("Expected /area to require authentication.", 401, httpResponse.getStatusLine().getStatusCode());

            request = new HttpGet(webAppURL.toURI() + "/area51");
            httpResponse = httpClient.execute(request);
            assertEquals("Expected /area51 to require authentication.", 401, httpResponse.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testSuccessfulAuthentication(@ArquillianResource URL webAppURL) throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

            HttpGet request = new HttpGet(webAppURL.toURI() + "area");
            request.addHeader(USERNAME_HEADER, USERNAME);
            request.addHeader(PASSWORD_HEADER, PASSWORD);

            HttpResponse httpResponse = httpClient.execute(request);
            assertEquals(200, httpResponse.getStatusLine().getStatusCode());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            httpResponse.getEntity().writeTo(bos);
            assertTrue(new String(bos.toByteArray(), StandardCharsets.UTF_8).contains("Welcome Area"));

            request = new HttpGet(webAppURL.toURI() + "area51");
            request.addHeader(USERNAME_HEADER, USERNAME);
            request.addHeader(PASSWORD_HEADER, PASSWORD);

            httpResponse = httpClient.execute(request);
            assertEquals(200, httpResponse.getStatusLine().getStatusCode());
            bos = new ByteArrayOutputStream();
            httpResponse.getEntity().writeTo(bos);
            assertTrue(new String(bos.toByteArray(), StandardCharsets.UTF_8).contains("Welcome Area51"));
        }
    }

    @Test
    public void testUnsuccessfulAuthentication(@ArquillianResource URL webAppURL) throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

            HttpGet request = new HttpGet(webAppURL.toURI() + "area");
            request.addHeader(USERNAME_HEADER, "evil");
            request.addHeader(PASSWORD_HEADER, "password");

            HttpResponse httpResponse = httpClient.execute(request);
            assertEquals(401, httpResponse.getStatusLine().getStatusCode());

            request = new HttpGet(webAppURL.toURI() + "area51");
            request.addHeader(USERNAME_HEADER, "evil");
            request.addHeader(PASSWORD_HEADER, "password");

            httpResponse = httpClient.execute(request);
            assertEquals(401, httpResponse.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testAuthNotRequired(@ArquillianResource URL webAppURL) throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet request = new HttpGet(webAppURL.toURI() + "index.jsp");

            HttpResponse httpResponse = httpClient.execute(request);
            assertEquals(200, httpResponse.getStatusLine().getStatusCode());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            httpResponse.getEntity().writeTo(bos);
            assertTrue(new String(bos.toByteArray(), StandardCharsets.UTF_8).contains("Unsecured"));
        }
    }

    static class ServerSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ConfigurableElement[] elements = new ConfigurableElement[3];
            // 1 - Add empty JACC Policy
            elements[0] = Policy.builder()
                    .withName("jacc")
                    .withJaccPolicy()
                    .build();

            // 2 - Map the application-security-domain
            elements[1] = UndertowApplicationSecurityDomain.builder()
                    .withName("SecurityAPI")
                    .withSecurityDomain("ApplicationDomain")
                    .withIntegratedJaspi(false)
                    .build();

            return elements;
        }

    }

}
