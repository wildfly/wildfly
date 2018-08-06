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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.jaspi.Flag;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;

/**
 * Test case testing a deployment secured using JASPI configured within the Elytron subsystem with the actual authentication
 * handled by the mapped SecurityDomain of the deployment.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ ConfiguredJaspiTestCase.ServerSetup.class })
public class ConfiguredJaspiTestCase extends JaspiTestBase {

    private static final String NAME = ConfiguredJaspiTestCase.class.getSimpleName();
    private static final String MODULE_NAME = "org.wildfly.security.examples.jaspic";

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return createDeployment(NAME);
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

    static class ServerSetup extends AbstractElytronSetupTask {

        private Path modulePath = null;

        @Override
        protected void setup(ModelControllerClient modelControllerClient) throws Exception {
            // Create the module jar.
            modulePath = createJar("jaspiSAM", SimpleServerAuthModule.class);
            super.setup(modelControllerClient);
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ConfigurableElement[] elements = new ConfigurableElement[3];
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
                    .build();
            // 3 - Add the jaspi-configuration
            elements[2] = JaspiConfiguration.builder()
                    .withName(NAME)
                    .withLayer("HttpServlet")
                    .withApplicationContext("default-host /" + NAME)
                    .withServerAuthModule("org.wildfly.test.integration.elytron.jaspi.SimpleServerAuthModule", MODULE_NAME, Flag.REQUIRED, Collections.singletonMap("mode", "integrated"))
                    .build();

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

    }
}
