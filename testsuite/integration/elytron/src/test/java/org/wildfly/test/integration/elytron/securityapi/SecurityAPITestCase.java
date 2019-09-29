/*
 * Copyright 2018 Red Hat, Inc.
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

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.wildfly.test.integration.elytron.securityapi.TestAuthenticationMechanism.MESSAGE;
import static org.wildfly.test.integration.elytron.securityapi.TestAuthenticationMechanism.MESSAGE_HEADER;
import static org.wildfly.test.integration.elytron.securityapi.TestAuthenticationMechanism.PASSWORD_HEADER;
import static org.wildfly.test.integration.elytron.securityapi.TestAuthenticationMechanism.USERNAME_HEADER;
import static org.wildfly.test.integration.elytron.securityapi.TestIdentityStore.PASSWORD;
import static org.wildfly.test.integration.elytron.securityapi.TestIdentityStore.USERNAME;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.net.URI;
import java.net.URL;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * Test case to test the EE Security API with WildFly Elytron.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ SecurityAPITestCase.ServerSetup.class })
public class SecurityAPITestCase {

    @ArquillianResource
    protected URL url;

    private final boolean ejbSupported = !Boolean.getBoolean("ts.layers");

    @Deployment
    protected static WebArchive createDeployment() {
        final Package testPackage = SecurityAPITestCase.class.getPackage();
        return ShrinkWrap.create(WebArchive.class, SecurityAPITestCase.class.getSimpleName() + ".war")
                .addClasses(SecurityAPITestCase.class, AbstractElytronSetupTask.class, ServerSetup.class)
                .addClasses(TestAuthenticationMechanism.class, TestIdentityStore.class)
                .addClasses(TestServlet.class)
                .addClasses(WhoAmI.class, WhoAmIBean.class)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset("SecurityAPI"), "jboss-web.xml")
                .addAsWebInfResource(testPackage, "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsWebInfResource(testPackage, "beans.xml", "beans.xml")
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("getSecurityDomain")), "permissions.xml");
    }

    @Test
    public void testCalls() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "/test"));

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Verify that we are not challenged.
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", "null", EntityUtils.toString(response.getEntity()));
            }

            request = new HttpGet(new URI(url.toExternalForm() + "/test?challenge=true"));
            // Now verify that we are challenged.
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_UNAUTHORIZED, statusCode);
                assertEquals("Unexpected challenge header", MESSAGE, response.getFirstHeader(MESSAGE_HEADER).getValue());
            }

            request = new HttpGet(new URI(url.toExternalForm() + "/test"));
            // Verify a bad username and password results in a challenge.
            request.addHeader(USERNAME_HEADER, "evil");
            request.addHeader(PASSWORD_HEADER, "password");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_UNAUTHORIZED, statusCode);
                assertEquals("Unexpected challenge header", MESSAGE, response.getFirstHeader(MESSAGE_HEADER).getValue());
            }

            // Verify a good username and password establishes an identity with the HttpServletRequest
            request = new HttpGet(new URI(url.toExternalForm() + "/test"));
            request.addHeader(USERNAME_HEADER, USERNAME);
            request.addHeader(PASSWORD_HEADER, PASSWORD);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", USERNAME, EntityUtils.toString(response.getEntity()));
            }

            // Verify a good username and password establishes an identity with the SecurityDomain
            request = new HttpGet(new URI(url.toExternalForm() + "/test?source=SecurityDomain"));
            request.addHeader(USERNAME_HEADER, USERNAME);
            request.addHeader(PASSWORD_HEADER, PASSWORD);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", USERNAME, EntityUtils.toString(response.getEntity()));
            }

            // Verify a good username and password establishes an identity with the SecurityContext
            request = new HttpGet(new URI(url.toExternalForm() + "/test?source=SecurityContext"));
            request.addHeader(USERNAME_HEADER, USERNAME);
            request.addHeader(PASSWORD_HEADER, PASSWORD);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", USERNAME, EntityUtils.toString(response.getEntity()));
            }

            if (ejbSupported) {
                // Verify a good username and password establishes an identity with the EJB SessionContext
                request = new HttpGet(new URI(url.toExternalForm() + "/test?ejb=true"));
                request.addHeader(USERNAME_HEADER, USERNAME);
                request.addHeader(PASSWORD_HEADER, PASSWORD);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                    assertEquals("Unexpected content of HTTP response.", USERNAME, EntityUtils.toString(response.getEntity()));
                }

                // Verify a good username and password establishes an identity with the SecurityDomain within an EJB
                request = new HttpGet(new URI(url.toExternalForm() + "/test?ejb=true&source=SecurityDomain"));
                request.addHeader(USERNAME_HEADER, USERNAME);
                request.addHeader(PASSWORD_HEADER, PASSWORD);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                    assertEquals("Unexpected content of HTTP response.", USERNAME, EntityUtils.toString(response.getEntity()));
                }

                // Verify a good username and password establishes an identity with the SecurityContext within an EJB
                request = new HttpGet(new URI(url.toExternalForm() + "/test?ejb=true&source=SecurityContext"));
                request.addHeader(USERNAME_HEADER, USERNAME);
                request.addHeader(PASSWORD_HEADER, PASSWORD);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                    assertEquals("Unexpected content of HTTP response.", USERNAME, EntityUtils.toString(response.getEntity()));
                }
            }

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
