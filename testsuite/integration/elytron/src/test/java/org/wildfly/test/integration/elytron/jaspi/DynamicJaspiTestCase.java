/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.elytron.jaspi;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URL;

import org.apache.http.Header;
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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * Test case testing a deployment secured using JASPI dynamically registered by the deployment.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ DynamicJaspiTestCase.ServerSetup.class })
public class DynamicJaspiTestCase extends JaspiTestBase {

    private static final String NAME = ConfiguredJaspiTestCase.class.getSimpleName();

    @ArquillianResource
    protected URL url;

    @Deployment
    protected static WebArchive createDeployment() {
        return createDeployment(NAME);
    }

    @Test
    public void testCalls() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm()));

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Verify that we are not challenged.
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", "null", EntityUtils.toString(response.getEntity()));
            }

            // Register JASPI Configuration
            request = new HttpGet(new URI(url.toExternalForm()) + "?action=register");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                Header[] challenge = response.getHeaders("X-MESSAGE");
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("No challenge headers expected", 0, challenge.length);
                assertEquals("Unexpected content of HTTP response.", "REGISTERED", EntityUtils.toString(response.getEntity()));
            }

            // Verify that we are now challenged.
            request = new HttpGet(new URI(url.toExternalForm()));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_UNAUTHORIZED, statusCode);
                Header[] challenge = response.getHeaders("X-MESSAGE");
                assertEquals("Only 1 header expected", 1, challenge.length);
                assertTrue("Challenge information contained in header.", challenge[0].getValue().contains("X-USERNAME"));
                assertTrue("Challenge information contained in header.", challenge[0].getValue().contains("X-PASSWORD"));
            }

            // Now authenticate.
            request.addHeader("X-USERNAME", "user1");
            request.addHeader("X-PASSWORD", "password1");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", "user1", EntityUtils.toString(response.getEntity()));
            }

            if (ejbSupported) {
                // Now try and EJB call
                request = new HttpGet(new URI(url.toExternalForm()) + "?action=ejb");
                request.addHeader("X-USERNAME", "user1");
                request.addHeader("X-PASSWORD", "password1");
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                    assertEquals("Unexpected content of HTTP response.", "user1", EntityUtils.toString(response.getEntity()));
                }
            }

            // Remove the registration
            request = new HttpGet(new URI(url.toExternalForm()) + "?action=remove");
            // Need to be authenticated to make the call.
            request.addHeader("X-USERNAME", "user1");
            request.addHeader("X-PASSWORD", "password1");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                Header[] challenge = response.getHeaders("X-MESSAGE");
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("No challenge headers expected", 0, challenge.length);
                assertEquals("Unexpected content of HTTP response.", "REMOVED", EntityUtils.toString(response.getEntity()));
            }

            // Verify that we are not challenged.
            request = new HttpGet(new URI(url.toExternalForm()));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", "null", EntityUtils.toString(response.getEntity()));
            }
        }
    }

    static class ServerSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ConfigurableElement[] elements = new ConfigurableElement[1];

            // 1 - Map the application-security-domain
            elements[0] = UndertowApplicationSecurityDomain.builder()
                    .withName("JaspiDomain")
                    .withSecurityDomain("ApplicationDomain")
                    .build();

            return elements;
        }

    }

}
