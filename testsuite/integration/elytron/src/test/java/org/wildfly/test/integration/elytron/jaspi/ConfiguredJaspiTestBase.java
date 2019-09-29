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

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.wildfly.test.integration.elytron.jaspi.SimpleServerAuthModule.ANONYMOUS;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assume;
import org.junit.Test;
import org.wildfly.security.auth.jaspi.Flag;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * A base for the configured JASPI testing allowing us to repeat the same set of tests across a different set of server
 * configurations.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
abstract class ConfiguredJaspiTestBase extends JaspiTestBase {

    private static final String MODULE_NAME = "org.wildfly.security.examples.jaspi";

    @Test
    public void testAnonymous() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm()));

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Verify that we are challenged.
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_UNAUTHORIZED, statusCode);
                Header[] challenge = response.getHeaders("X-MESSAGE");
                assertEquals("Only 1 header expected", 1, challenge.length);
                assertTrue("Challenge information contained in header.", challenge[0].getValue().contains("X-USERNAME"));
                assertTrue("Challenge information contained in header.", challenge[0].getValue().contains("X-PASSWORD"));
            }

            // Now authenticate.
            request.addHeader("X-USERNAME", ANONYMOUS);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", "null", EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSuccess() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role1"));

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Verify that we are challenged.
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

            request = new HttpGet(new URI(url.toExternalForm() + "role1?value=authType"));
            request.addHeader("X-USERNAME", "user1");
            request.addHeader("X-PASSWORD", "password1");
            request.addHeader("X-AUTH-TYPE", "TestAuth");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", "TestAuth", EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSuccess_Session() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role1"));

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Verify that we are challenged.
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
            request.addHeader("X-AUTH-TYPE", "SessionAuth");
            request.addHeader("X-SESSION", "register");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", "user1", EntityUtils.toString(response.getEntity()));
            }
            // Repeat without headers
            request = new HttpGet(new URI(url.toExternalForm() + "role1"));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", "user1", EntityUtils.toString(response.getEntity()));
            }

            // Was the authType saved?
            request = new HttpGet(new URI(url.toExternalForm() + "role1?value=authType"));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", "SessionAuth", EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSuccess_EJB() throws Exception {

        Assume.assumeTrue("EJB is not supported on the server; disabling ejb test aspects", ejbSupported);

        final HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role1?action=ejb"));

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Verify that we are challenged.
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
        }
    }

    @Test
    public void testInsufficientRole() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role2"));

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Verify that we are challenged.
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
                assertEquals("Unexpected status code in HTTP response.", SC_FORBIDDEN, statusCode);
            }

            // Now try adding the role.
            request.addHeader("X-ROLES", "Role1,Role2");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", "user1", EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testInvalidPrincipal() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role1"));

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Verify that we are challenged.
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_UNAUTHORIZED, statusCode);
                Header[] challenge = response.getHeaders("X-MESSAGE");
                assertEquals("Only 1 header expected", 1, challenge.length);
                assertTrue("Challenge information contained in header.", challenge[0].getValue().contains("X-USERNAME"));
                assertTrue("Challenge information contained in header.", challenge[0].getValue().contains("X-PASSWORD"));
            }

            // Now authenticate.
            request.addHeader("X-USERNAME", "user1wrong");
            request.addHeader("X-PASSWORD", "password1");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_UNAUTHORIZED, statusCode);
            }
        }
    }

    @Test
    public void testInvalidCredential() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role1"));
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Verify that we are challenged.
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
            request.addHeader("X-PASSWORD", "password1wrong");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_UNAUTHORIZED, statusCode);
            }
        }
    }

    abstract static class ServerSetup extends AbstractElytronSetupTask {

        private Path modulePath = null;

        @Override
        protected void setup(ModelControllerClient modelControllerClient) throws Exception {
            // Create the module jar.
            modulePath = createJar("jaspiSAM", SimpleServerAuthModule.class);
            super.setup(modelControllerClient);
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ConfigurableElement[] elements = new ConfigurableElement[enableAnonymousLogin() ? 4 : 3];
            // 1 - Register the module
            elements[0] = Module.builder()
                    .withName(MODULE_NAME)
                    .withResource(modulePath.toAbsolutePath().toString())
                    .withDependency("javax.security.auth.message.api")
                    .withDependency("javax.servlet.api")
                    .withDependency("javax.api")
                    .withDependency("org.wildfly.security.elytron")
                    .build();
            // 2 - Map the application-security-domain
            elements[1] = UndertowApplicationSecurityDomain.builder()
                    .withName("JaspiDomain")
                    .withSecurityDomain("ApplicationDomain")
                    .withIntegratedJaspi(isIntegratedJaspi())
                    .build();
            // 3 - Add the jaspi-configuration
            elements[2] = JaspiConfiguration.builder()
                    .withName(getName())
                    .withLayer("HttpServlet")
                    .withApplicationContext("default-host /" + getName())
                    .withServerAuthModule("org.wildfly.test.integration.elytron.jaspi.SimpleServerAuthModule", MODULE_NAME, Flag.REQUIRED, getOptions())
                    .build();
            if (enableAnonymousLogin()) {
                elements[3] = new ConfigurableElement() {

                    private final PathAddress ADDRESS = PathAddress.pathAddress(PathElement.pathElement("subsystem", "elytron"), PathElement.pathElement("simple-permission-mapper", "default-permission-mapper"));

                    @Override
                    public String getName() {
                        return "Enable LoginPermission for anonymous.";
                    }

                    @Override
                    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
                        ModelNode write = Util.getWriteAttributeOperation(ADDRESS, "mapping-mode", "or");
                        Utils.applyUpdate(write, client);
                    }

                    @Override
                    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
                        ModelNode write = Util.getWriteAttributeOperation(ADDRESS, "mapping-mode", "first");
                        Utils.applyUpdate(write, client);
                    }

                };
            }

            return elements;
        }

        @Override
        protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
            super.tearDown(modelControllerClient);

            Files.deleteIfExists(modulePath);
        }

        static Path createJar(String namePrefix, Class<?>... classes) throws IOException {
            Path testJar = Files.createTempFile(namePrefix, ".jar");
            JavaArchive jar = ShrinkWrap.create(JavaArchive.class).addClasses(classes);
            jar.as(ZipExporter.class).exportTo(testJar.toFile(), true);
            return testJar;
        }

        protected abstract String getName();

        protected Map<String, String> getOptions() {
            return Collections.singletonMap("mode", getMode());
        }

        protected String getMode() {
            return "integrated";
        }

        protected boolean isIntegratedJaspi() {
            return true;
        }

        protected boolean enableAnonymousLogin() {
            return false;
        }

    }

}
