/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.securityapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * Validates that the {@code ee-security} subsystem is enabled when a deployment implements Jakarta Security (ex.
 * implementing an {@link jakarta.security.enterprise.identitystore.IdentityStore IdentityStore}). Returns a custom
 * principal to client via {@link jakarta.security.enterprise.SecurityContext}.
 *
 * @author <a href="mailto:jrodri@redhat.com">Jessica Rodriguez</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ EESecurityInjectionEnabledJakartaTestCase.ServerSetup.class })
public class EESecurityInjectionEnabledJakartaTestCase extends EESecurityInjectionEnabledAbstractTestCase {

    @ArquillianResource
    private Deployer deployer;
    private static final String WEB_APP_NAME = "WFLY-17541-Jakarta";

    @Deployment(name = WEB_APP_NAME, managed = false, testable = false)
    public static WebArchive warDeployment() {
        Class<EESecurityInjectionEnabledJakartaTestCase> testClass = EESecurityInjectionEnabledJakartaTestCase.class;
        return ShrinkWrap.create(WebArchive.class,testClass.getSimpleName() + ".war")
                .addClasses(testClass, EESecurityInjectionEnabledAbstractTestCase.class)
                .addClasses(ServerSetup.class, EESecurityInjectionEnabledAbstractTestCase.ServerSetup.class,
                        AbstractElytronSetupTask.class)
                .addClasses(TestInjectServlet.class)
                .addClasses(TestAuthenticationMechanism.class, TestIdentityStoreCustomWrapper.class, TestIdentityStore.class)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(TEST_APP_DOMAIN), "jboss-web.xml")
                .addAsWebInfResource(testClass.getPackage(), "WFLY-17541-jakarta-web.xml", "web.xml")
                .addAsWebInfResource(testClass.getPackage(), "WFLY-17541-index.xml", "index.xml")
                .addAsManifestResource(new StringAsset("Dependencies: " + MODULE_NAME + "\n"), "MANIFEST.MF");
    }

    @Override
    Header[] setRequestAuthHeader() {
        return new BasicHeader[] {
                new BasicHeader("X-USERNAME", "user1"),
                new BasicHeader("X-PASSWORD", "password1")
        };
    }

    @Test @InSequence(1)
    public void deployApplication() {
        deployer.deploy(WEB_APP_NAME);
    }

    @Test @InSequence(2)
    public void testUnsuccessfulAuthentication(@ArquillianResource URL webAppURL) throws IOException, URISyntaxException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet request = new HttpGet(webAppURL.toURI() + "/inject");
            HttpResponse response = httpClient.execute(request);
            assertEquals(401, response.getStatusLine().getStatusCode());

            // Check that the challenge message was sent
            Header authHeader = response.getFirstHeader(TestAuthenticationMechanism.MESSAGE_HEADER);

            assertNotNull(authHeader);
            assertEquals(TestAuthenticationMechanism.MESSAGE, authHeader.getValue());
        }
    }

    @Override
    @Test @InSequence(3)
    public void testCustomPrincipalWithInject(@ArquillianResource URL webAppURL) throws IOException, URISyntaxException {
        super.testCustomPrincipalWithInject(webAppURL);
    }

    @Test @InSequence(4)
    public void undeployApplication() {
        deployer.undeploy(WEB_APP_NAME);
    }

    static class ServerSetup extends EESecurityInjectionEnabledAbstractTestCase.ServerSetup {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ConfigurableElement[] elements  = new ConfigurableElement[3];
            // Add module with custom principal and principal transformer
            elements[0] = module;

            // Create security domain with default permission mapper
            elements[1] = SimpleSecurityDomain.builder()
                    .withName(TEST_SECURITY_DOMAIN)
                    .withPermissionMapper(DEFAULT_PERMISSION_MAPPER)
                    .build();

            // Add security domain to Undertow configuration
            elements[2] = UndertowApplicationSecurityDomain.builder()
                    .withName(TEST_APP_DOMAIN)
                    .withSecurityDomain(TEST_SECURITY_DOMAIN)
                    .withIntegratedJaspi(false)
                    .build();

            return elements;
        }
    }
}
