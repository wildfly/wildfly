/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.securityapi;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.Module;

/**
 * Validate that the {@code ee-security} subsystem is enabled when a deployment uses the injection
 * of {@link jakarta.security.enterprise.SecurityContext}. Allows for Jakarta Security components to
 * be used without a full implementation. Modified from {@link EESecurityAuthMechanismMultiConstraintsTestCase}.
 *
 * @see <a href="https://issues.redhat.com/browse/WFLY-17541">WFLY-17541</a>
 * @author <a href="mailto:jrodri@redhat.com">Jessica Rodriguez</a>
 */

@RunWith(Arquillian.class)
@RunAsClient
public abstract class EESecurityInjectionEnabledAbstractTestCase {
    static final String MODULE_NAME = "org.wildfly.security.examples.custom-principal";
    static final String TEST_APP_DOMAIN = "testApplicationDomain";

    public void testCustomPrincipalWithInject(URL webAppURL) throws IOException, URISyntaxException {
        testCustomPrincipalInternal(webAppURL);
    }

    /**
     * @return the header to be used for authentication with the deployed web app
     */
    abstract Header[] setRequestAuthHeader();

    private void testCustomPrincipalInternal(URL webAppURL) throws IOException, URISyntaxException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            String requestURI = webAppURL.toURI() + "/inject";
            HttpGet request = new HttpGet(requestURI);
            request.setHeaders(setRequestAuthHeader());
            HttpResponse response = httpClient.execute(request);
            assertEquals(200, response.getStatusLine().getStatusCode());

            ByteArrayOutputStream baosBody = new ByteArrayOutputStream();
            response.getEntity().writeTo(baosBody);
            String responseBody = baosBody.toString(StandardCharsets.UTF_8);

            // Check that custom principal was retrieved
            assertTrue(responseBody.contains("user1"));
            assertTrue(responseBody.contains(TestCustomPrincipal.class.getCanonicalName()));

            // Check that custom method was accessed and returned a valid date
            Header loginHeader = response.getFirstHeader(TestInjectServlet.LOGIN_HEADER);
            assertNotNull(loginHeader);
            try {
                LocalDateTime.parse(loginHeader.getValue(), ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new DateTimeParseException("Login timestamp returned from servlet was not a valid ISO-format LocalDateTime",
                        e.getParsedString(), e.getErrorIndex(), e);
            }

        }
    }

    abstract static class ServerSetup extends AbstractElytronSetupTask {

        static final String TEST_CUSTOM_PRINCIPAL_TRANSFORMER = "testCustomPrincipalTransformer";
        static final String TEST_SECURITY_DOMAIN = "testSecurityDomain";
        static final String DEFAULT_PERMISSION_MAPPER = "default-permission-mapper";

        static Path modulePath;
        static ConfigurableElement module;

        static Path createJar(String namePrefix, Class<?>... classes) throws IOException {
            Path testJar = Files.createTempFile(namePrefix, ".jar");
            JavaArchive jar = ShrinkWrap.create(JavaArchive.class).addClasses(classes);
            jar.as(ZipExporter.class).exportTo(testJar.toFile(), true);
            return testJar;
        }

        @Override
        protected void setup(ModelControllerClient modelControllerClient) throws Exception {
            modulePath = createJar("custom-principal",
                    TestCustomPrincipal.class, TestCustomPrincipalTransformer.class);

            module = Module.builder()
                    .withName(MODULE_NAME)
                    .withResource(modulePath.toAbsolutePath().toString())
                    .withDependency("jakarta.security.enterprise.api")
                    .withDependency("org.wildfly.security.elytron")
                    .withDependency("org.wildfly.extension.elytron")
                    .build();

            super.setup(modelControllerClient);
        }

        @Override
        protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
            super.tearDown(modelControllerClient);

            Files.deleteIfExists(modulePath);
        }
    }
}