/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.oidc.client.deployment;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.getRealmRepresentation;

import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration;
import org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest;

import io.restassured.RestAssured;

/**
 * Tests for the OpenID Connect authentication mechanism.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ OidcWithDeploymentConfigTest.KeycloakAndSystemPropertySetup.class })
public class OidcWithDeploymentConfigTest extends OidcBaseTest {

    private static final String OIDC_PROVIDER_URL = "oidc.provider.url";
    private static final String OIDC_JSON_WITH_PROVIDER_URL_FILE = "OidcWithProviderUrl.json";

    private static final String OIDC_AUTH_SERVER_URL = "oidc.auth.server.url";
    private static final String OIDC_JSON_WITH_AUTH_SERVER_URL_FILE = "OidcWithAuthServerUrl.json";

    private static final String WRONG_OIDC_PROVIDER_URL = "wrong.oidc.provider.url";
    private static final String OIDC_JSON_WITH_WRONG_PROVIDER_URL_FILE = "OidcWithWrongProviderUrl.json";

    private static final String OIDC_JSON_WITH_WRONG_SECRET_FILE = "OidcWithWrongSecret.json";

    private static final String MISSING_EXPRESSION_APP = "MissingExpressionOidcApp";
    private static final String OIDC_JSON_WITH_MISSING_EXPRESSION_FILE = "OidcWithMissingExpression.json";

    private static final String BEARER_ONLY_WITH_AUTH_SERVER_URL_FILE = "BearerOnlyWithAuthServerUrl.json";

    private static final String BEARER_ONLY_WITH_PROVIDER_URL_FILE = "BearerOnlyWithProviderUrl.json";
    private static final String BASIC_AUTH_WITH_PROVIDER_URL_FILE = "BasicAuthWithProviderUrl.json";
    private static final String CORS_WITH_PROVIDER_URL_FILE = "CorsWithProviderUrl.json";

    private static Map<String, KeycloakConfiguration.ClientAppType> APP_NAMES;
    static {
        APP_NAMES = new HashMap<>();
        APP_NAMES.put(PROVIDER_URL_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(AUTH_SERVER_URL_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(WRONG_PROVIDER_URL_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(WRONG_SECRET_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(MISSING_EXPRESSION_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(DIRECT_ACCCESS_GRANT_ENABLED_CLIENT, KeycloakConfiguration.ClientAppType.DIRECT_ACCESS_GRANT_OIDC_CLIENT);
        APP_NAMES.put(BEARER_ONLY_AUTH_SERVER_URL_APP, KeycloakConfiguration.ClientAppType.BEARER_ONLY_CLIENT);
        APP_NAMES.put(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ClientAppType.BEARER_ONLY_CLIENT);
        APP_NAMES.put(BASIC_AUTH_PROVIDER_URL_APP, KeycloakConfiguration.ClientAppType.BEARER_ONLY_CLIENT);
        APP_NAMES.put(CORS_PROVIDER_URL_APP, KeycloakConfiguration.ClientAppType.BEARER_ONLY_CLIENT);
        APP_NAMES.put(CORS_CLIENT, KeycloakConfiguration.ClientAppType.CORS_CLIENT);
    }

    @ArquillianResource
    protected static Deployer deployer;

    @Deployment(name = PROVIDER_URL_APP, managed = false, testable = false)
    public static WebArchive createProviderUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, PROVIDER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_JSON_WITH_PROVIDER_URL_FILE, "oidc.json");
    }

    @Deployment(name = AUTH_SERVER_URL_APP, managed = false, testable = false)
    public static WebArchive createAuthServerUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, AUTH_SERVER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_JSON_WITH_AUTH_SERVER_URL_FILE, "oidc.json");
    }

    @Deployment(name = WRONG_PROVIDER_URL_APP, managed = false, testable = false)
    public static WebArchive createWrongProviderUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, WRONG_PROVIDER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_JSON_WITH_WRONG_PROVIDER_URL_FILE, "oidc.json");
    }

    @Deployment(name = WRONG_SECRET_APP, managed = false, testable = false)
    public static WebArchive createWrongSecretDeployment() {
        return ShrinkWrap.create(WebArchive.class, WRONG_SECRET_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_JSON_WITH_WRONG_SECRET_FILE, "oidc.json");
    }

    @Deployment(name = MISSING_EXPRESSION_APP, managed = false, testable = false)
    public static WebArchive createMissingExpressionDeployment() {
        return ShrinkWrap.create(WebArchive.class, MISSING_EXPRESSION_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_JSON_WITH_MISSING_EXPRESSION_FILE, "oidc.json");
    }

    @Deployment(name = BEARER_ONLY_AUTH_SERVER_URL_APP, managed = false, testable = false)
    public static WebArchive createBearerOnlyAuthServerUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, BEARER_ONLY_AUTH_SERVER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), BEARER_ONLY_WITH_AUTH_SERVER_URL_FILE, "oidc.json");
    }

    @Deployment(name = BEARER_ONLY_PROVIDER_URL_APP, managed = false, testable = false)
    public static WebArchive createBearerOnlyProviderUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, BEARER_ONLY_PROVIDER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), BEARER_ONLY_WITH_PROVIDER_URL_FILE, "oidc.json");
    }

    @Deployment(name = BASIC_AUTH_PROVIDER_URL_APP, managed = false, testable = false)
    public static WebArchive createBasicAuthProviderUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, BASIC_AUTH_PROVIDER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), BASIC_AUTH_WITH_PROVIDER_URL_FILE, "oidc.json");
    }

    @Deployment(name = CORS_PROVIDER_URL_APP, managed = false, testable = false)
    public static WebArchive createCorsProviderUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, CORS_PROVIDER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), CORS_WITH_PROVIDER_URL_FILE, "oidc.json");
    }

    @Test
    @InSequence(1)
    public void testWrongPasswordWithProviderUrl() throws Exception {
        deployer.deploy(PROVIDER_URL_APP);
        super.testWrongPasswordWithProviderUrl();
    }

    @Test
    @InSequence(2)
    public void testSucessfulAuthenticationWithProviderUrl() throws Exception {
        super.testSucessfulAuthenticationWithProviderUrl();
    }

    @Test
    @InSequence(3)
    public void testWrongRoleWithProviderUrl() throws Exception {
        super.testWrongRoleWithProviderUrl();
    }

    @Test
    @InSequence(4)
    public void testTokenProvidedBearerOnlyNotSet() throws Exception {
        super.testTokenProvidedBearerOnlyNotSet();
    }

    @Test
    @InSequence(5)
    public void testTokenNotProvidedBearerOnlyNotSet() throws Exception {
        super.testTokenNotProvidedBearerOnlyNotSet();
    }

    @Test
    @InSequence(6)
    public void testBasicAuthenticationWithoutEnableBasicAuthSetAndWithoutBearerOnlySet() throws Exception {
        try {
            super.testBasicAuthenticationWithoutEnableBasicAuthSetAndWithoutBearerOnlySet();
        } finally {
            deployer.undeploy(PROVIDER_URL_APP);
        }
    }

    @Test
    @InSequence(7)
    public void testWrongPasswordWithAuthServerUrl() throws Exception {
        deployer.deploy(AUTH_SERVER_URL_APP);
        super.testWrongPasswordWithAuthServerUrl();
    }

    @Test
    @InSequence(8)
    public void testSucessfulAuthenticationWithAuthServerUrl() throws Exception {
        super.testSucessfulAuthenticationWithAuthServerUrl();
    }

    @Test
    @InSequence(9)
    public void testWrongRoleWithAuthServerUrl() throws Exception {
        try {
            super.testWrongRoleWithAuthServerUrl();
        } finally {
            deployer.undeploy(AUTH_SERVER_URL_APP);
        }
    }

    @Test
    public void testWrongProviderUrl() throws Exception {
        try {
            deployer.deploy(WRONG_PROVIDER_URL_APP);
            super.testWrongProviderUrl();
        } finally {
            deployer.undeploy(WRONG_PROVIDER_URL_APP);
        }
    }

    @Test
    public void testWrongClientSecret() throws Exception {
        try {
            deployer.deploy(WRONG_SECRET_APP);
            super.testWrongClientSecret();
        } finally {
            deployer.undeploy(WRONG_SECRET_APP);
        }
    }

    @Test
    public void testMissingExpression() throws Exception {
        deployer.deploy(MISSING_EXPRESSION_APP);
        try {
            loginToApp(MISSING_EXPRESSION_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, -1, null, false);
        } finally {
            deployer.undeploy(MISSING_EXPRESSION_APP);
        }
    }

    @Test
    @InSequence(10)
    public void testSucessfulBearerOnlyAuthenticationWithAuthServerUrl() throws Exception {
        deployer.deploy(BEARER_ONLY_AUTH_SERVER_URL_APP);
        super.testSucessfulBearerOnlyAuthenticationWithAuthServerUrl();

    }

    @Test
    @InSequence(11)
    public void testNoTokenProvidedWithAuthServerUrl() throws Exception {
        try {
            super.testNoTokenProvidedWithAuthServerUrl();
        } finally {
            deployer.undeploy(BEARER_ONLY_AUTH_SERVER_URL_APP);
        }
    }

    @Test
    @InSequence(12)
    public void testSucessfulBearerOnlyAuthenticationWithProviderUrl() throws Exception {
        deployer.deploy(BEARER_ONLY_PROVIDER_URL_APP);
        super.testSucessfulBearerOnlyAuthenticationWithProviderUrl();
    }

    @Test
    @InSequence(13)
    public void testWrongToken() throws Exception {
        super.testWrongToken();
    }

    @Test
    @InSequence(14)
    public void testInvalidToken() throws Exception {
        super.testInvalidToken();
    }

    @Test
    @InSequence(15)
    public void testNoTokenProvidedWithProviderUrl() throws Exception {
        super.testNoTokenProvidedWithProviderUrl();
    }

    @Test
    @InSequence(16)
    public void testValidTokenViaQueryParameter() throws Exception {
        super.testValidTokenViaQueryParameter();
    }

    @Test
    @InSequence(17)
    public void testWrongTokenViaQueryParameter() throws Exception {
        super.testWrongTokenViaQueryParameter();
    }

    @Test
    @InSequence(18)
    public void testInvalidTokenViaQueryParameter() throws Exception {
        super.testInvalidTokenViaQueryParameter();
    }

    @Test
    @InSequence(19)
    public void testBasicAuthenticationWithoutEnableBasicAuthSet() throws Exception {
        super.testBasicAuthenticationWithoutEnableBasicAuthSet();
    }

    @Test
    @InSequence(20)
    public void testCorsRequestWithoutEnableCors() throws Exception {
        try {
            super.testCorsRequestWithoutEnableCors();
        } finally {
            deployer.undeploy(BEARER_ONLY_PROVIDER_URL_APP);
        }
    }

    @Test
    @InSequence(21)
    public void testValidCredentialsBasicAuthentication() throws Exception {
        deployer.deploy(BASIC_AUTH_PROVIDER_URL_APP);
        super.testValidCredentialsBasicAuthentication();
    }

    @Test
    @InSequence(22)
    public void testInvalidCredentialsBasicAuthentication() throws Exception {
        try {
            super.testInvalidCredentialsBasicAuthentication();
        } finally {
            deployer.undeploy(BASIC_AUTH_PROVIDER_URL_APP);
        }
    }

    @Test
    @InSequence(23)
    public void testCorsRequestWithEnableCors() throws Exception {
        deployer.deploy(CORS_PROVIDER_URL_APP);
        super.testCorsRequestWithEnableCors();
    }

    @Test
    @InSequence(24)
    public void testCorsRequestWithEnableCorsWithWrongToken() throws Exception {
        super.testCorsRequestWithEnableCorsWithWrongToken();
    }

    @Test
    @InSequence(25)
    public void testCorsRequestWithEnableCorsWithInvalidToken() throws Exception {
        super.testCorsRequestWithEnableCorsWithInvalidToken();
    }

    @Test
    @InSequence(26)
    public void testCorsRequestWithEnableCorsWithInvalidOrigin() throws Exception {
        try {
            super.testCorsRequestWithEnableCorsWithInvalidOrigin();
        } finally {
            deployer.undeploy(CORS_PROVIDER_URL_APP);
        }
    }

    static class KeycloakAndSystemPropertySetup extends KeycloakSetup {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            super.setup(managementClient, containerId);
            sendRealmCreationRequest(getRealmRepresentation(TEST_REALM, CLIENT_SECRET, CLIENT_HOST_NAME, CLIENT_PORT, APP_NAMES));

            ModelControllerClient client = managementClient.getControllerClient();
            ModelNode operation = createOpNode("system-property=" + OIDC_PROVIDER_URL, ModelDescriptionConstants.ADD);
            operation.get("value").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM);
            Utils.applyUpdate(operation, client);

            operation = createOpNode("system-property=" + OIDC_AUTH_SERVER_URL, ModelDescriptionConstants.ADD);
            operation.get("value").set(KEYCLOAK_CONTAINER.getAuthServerUrl());
            Utils.applyUpdate(operation, client);

            operation = createOpNode("system-property=" + WRONG_OIDC_PROVIDER_URL, ModelDescriptionConstants.ADD);
            operation.get("value").set("http://fakeauthserver/auth"); // provider url does not exist
            Utils.applyUpdate(operation, client);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            RestAssured
                    .given()
                    .auth().oauth2(org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.getAdminAccessToken(KEYCLOAK_CONTAINER.getAuthServerUrl()))
                    .when()
                    .delete(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/admin/realms/" + TEST_REALM).then().statusCode(204);

            super.tearDown(managementClient, containerId);
            ModelControllerClient client = managementClient.getControllerClient();
            ModelNode operation = createOpNode("system-property=" + OIDC_PROVIDER_URL, ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, client);

            operation = createOpNode("system-property=" + OIDC_AUTH_SERVER_URL, ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, client);

            operation = createOpNode("system-property=" + WRONG_OIDC_PROVIDER_URL, ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, client);
        }
    }
}
