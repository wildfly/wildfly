/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.securityapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.Header;
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
import org.wildfly.test.security.common.elytron.CustomPrincipalTransformer;
import org.wildfly.test.security.common.elytron.FileSystemRealm;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;
import org.wildfly.test.security.common.elytron.MechanismRealmConfiguration;
import org.wildfly.test.security.common.elytron.SimpleHttpAuthenticationFactory;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.UserWithAttributeValues;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * Validates that the {@code ee-security} subsystem is enabled when a deployment does not fully implement Jakarta Security,
 * but still uses parts of the API. Here, a custom principal is returned to the client via
 * {@link jakarta.security.enterprise.SecurityContext}, while authentication is performed by Elytron.
 *
 * @author <a href="mailto:jrodri@redhat.com">Jessica Rodriguez</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ EESecurityInjectionEnabledElytronTestCase.ServerSetup.class })
public class EESecurityInjectionEnabledElytronTestCase extends EESecurityInjectionEnabledAbstractTestCase {

    @ArquillianResource
    private Deployer deployer;
    private static final String WEB_APP_NAME = "WFLY-17541-Elytron";

    @Deployment(name = WEB_APP_NAME, managed = false, testable = false)
    public static WebArchive warDeployment() {
        Class<EESecurityInjectionEnabledElytronTestCase> testClass = EESecurityInjectionEnabledElytronTestCase.class;
        return ShrinkWrap.create(WebArchive.class, testClass.getSimpleName() + ".war")
                .addClasses(testClass, EESecurityInjectionEnabledAbstractTestCase.class)
                .addClasses(ServerSetup.class, EESecurityInjectionEnabledAbstractTestCase.ServerSetup.class,
                        AbstractElytronSetupTask.class)
                .addClasses(TestInjectElytronServlet.class, TestInjectServlet.class)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(TEST_APP_DOMAIN), "jboss-web.xml")
                .addAsWebInfResource(testClass.getPackage(), "WFLY-17541-elytron-web.xml", "web.xml")
                .addAsWebInfResource(testClass.getPackage(), "WFLY-17541-index.xml", "index.xml")
                .addAsWebInfResource(testClass.getPackage(), "beans.xml", "beans.xml")
                .addAsManifestResource(new StringAsset("Dependencies: " + MODULE_NAME + "\n"), "MANIFEST.MF");
    }

    @Override
    Header[] setRequestAuthHeader() {
        // BASIC authentication - base64(username:password)
        return new BasicHeader[] { new BasicHeader("Authorization", "Basic dXNlcjE6cGFzc3dvcmQx") };
    }

    @Test @InSequence(1)
    public void deployApplication() {
        deployer.deploy(WEB_APP_NAME);
    }

    @Override
    @Test @InSequence(2)
    public void testCustomPrincipalWithInject(@ArquillianResource URL webAppURL) throws IOException, URISyntaxException {
        super.testCustomPrincipalWithInject(webAppURL);
    }

    @Test @InSequence(3)
    public void undeployApplication() {
        deployer.undeploy(WEB_APP_NAME);
    }

    static class ServerSetup extends EESecurityInjectionEnabledAbstractTestCase.ServerSetup {
        static final String TEST_REALM = "testRealm";
        static final String TEST_HTTP_FACTORY = "testHttpFactory";

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ConfigurableElement[] elements =  new ConfigurableElement[6];
            // Add module with custom principal and principal transformer
            elements[0] = module;

            // Create filesystem security realm with one identity
            elements[1] = FileSystemRealm.builder()
                    .withName(TEST_REALM)
                    .withUser(UserWithAttributeValues.builder()
                            .withName("user1")
                            .withPassword("password1")
                            .withValues("Login")
                            .build())
                    .build();

            // Add custom pre-realm principal transformer to create custom principal
            elements[2] = CustomPrincipalTransformer.builder()
                    .withName(TEST_CUSTOM_PRINCIPAL_TRANSFORMER)
                    .withModule(MODULE_NAME)
                    .withClassName(TestCustomPrincipalTransformer.class.getCanonicalName())
                    .build();

            // Create security domain using security realm and principal transformer
            elements[3] = SimpleSecurityDomain.builder()
                    .withName(TEST_SECURITY_DOMAIN)
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm(TEST_REALM)
                            .build())
                    .withDefaultRealm(TEST_REALM)
                    .withPermissionMapper(DEFAULT_PERMISSION_MAPPER)
                    .build();

            // Create HTTP authentication factory
            elements[4] = SimpleHttpAuthenticationFactory.builder()
                    .withName(TEST_HTTP_FACTORY)
                    .withHttpServerMechanismFactory("global")
                    .withSecurityDomain(TEST_SECURITY_DOMAIN)
                    .addMechanismConfiguration(MechanismConfiguration.builder()
                            .withMechanismName("BASIC")
                            .addMechanismRealmConfiguration(MechanismRealmConfiguration.builder()
                                    .withRealmName(TEST_REALM)
                                    .build())
                            .withPreRealmPrincipalTransformer(TEST_CUSTOM_PRINCIPAL_TRANSFORMER)
                            .build())
                    .build();

            // Add HTTP authentication factory to Undertow configuration
            elements[5] = UndertowApplicationSecurityDomain.builder()
                    .withName(TEST_APP_DOMAIN)
                    .httpAuthenticationFactory(TEST_HTTP_FACTORY)
                    .withEnableJacc(true)
                    .build();

            return elements;
        }
    }
}
