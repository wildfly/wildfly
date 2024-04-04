/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.manual.elytron.oidc.deployment;

import java.util.HashMap;
import java.util.Map;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.wildfly.test.manual.elytron.oidc.KeycloakConfiguration.getAdminAccessToken;
import static org.wildfly.test.manual.elytron.oidc.KeycloakConfiguration.getRealmRepresentation;

import io.restassured.RestAssured;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.manual.elytron.oidc.KeycloakConfiguration;
import org.wildfly.test.manual.elytron.oidc.OidcBaseTest;
import org.wildfly.test.manual.elytron.oidc.subsystem.SimpleServletWithScope;

/**
 * Tests for the scope attribute in OpenID Connect authentication
 * mechanism using the preview stability level.
 *
 * @author <a href="mailto:prpaul@redhat.com">Prarthona Paul</a>
 */

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ OidcWithDeploymentConfigTest.KeycloakAndSystemPropertySetup.class })
public class OidcWithDeploymentConfigTest extends OidcBaseTest {

    private static final String OIDC_PROVIDER_URL = "oidc.provider.url";
    private static final String WRONG_OIDC_PROVIDER_URL = "wrong.oidc.provider.url";

    private static final String OIDC_AUTH_SERVER_URL = "oidc.auth.server.url";
    private static final String SINGLE_SCOPE_FILE = "OidcWithSingleScope.json";
    private static final String MULTI_SCOPE_FILE = "OidcWithMultipleScopes.json";
    private static final String INVALID_SCOPE_FILE = "OidcWithInvalidScope.json";
    private static final String OPENID_SCOPE_FILE = "OidcWithOpenIDScope.json";

    private static Map<String, KeycloakConfiguration.ClientAppType> APP_NAMES;
    static {
        APP_NAMES = new HashMap<>();
        APP_NAMES.put(SINGLE_SCOPE_APP, KeycloakConfiguration.ClientAppType.OIDC_SCOPE_CLIENT);
        APP_NAMES.put(MULTIPLE_SCOPE_APP, KeycloakConfiguration.ClientAppType.OIDC_SCOPE_CLIENT);
        APP_NAMES.put(INVALID_SCOPE_APP, KeycloakConfiguration.ClientAppType.OIDC_SCOPE_CLIENT);
        APP_NAMES.put(OPENID_SCOPE_APP, KeycloakConfiguration.ClientAppType.OIDC_SCOPE_CLIENT);
    }

    @ArquillianResource
    protected static Deployer deployer;

    @Deployment(name = SINGLE_SCOPE_APP, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static WebArchive createSingleScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, SINGLE_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), SINGLE_SCOPE_FILE, "oidc.json");
    }

    @Deployment(name = MULTIPLE_SCOPE_APP, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static WebArchive createMultipleScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, MULTIPLE_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), MULTI_SCOPE_FILE, "oidc.json");
    }

    @Deployment(name = INVALID_SCOPE_APP, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static WebArchive createInvalidScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, INVALID_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), INVALID_SCOPE_FILE, "oidc.json");
    }

    @Deployment(name = OPENID_SCOPE_APP, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static WebArchive createOpenIdScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, OPENID_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OPENID_SCOPE_FILE, "oidc.json");
    }

    @Test
    public void testOpenIDScope() throws Exception {
        try{
            deployer.deploy(OPENID_SCOPE_APP);
            super.testOpenIDScope();
        } finally {
            deployer.undeploy(OPENID_SCOPE_APP);
        }
    }

    @Test
    public void testSingleScope() throws Exception {
        try {
            deployer.deploy(SINGLE_SCOPE_APP);
            super.testSingleScope();
        } finally {
            deployer.undeploy(SINGLE_SCOPE_APP);
        }
    }

    @Test
    public void testMultipleScope() throws Exception {
        try {
            deployer.deploy(MULTIPLE_SCOPE_APP);
            super.testMultipleScope();
        } finally {
            deployer.undeploy(MULTIPLE_SCOPE_APP);
        }
    }

    @Test
    public void testInvalidScope() throws Exception {
        try {
            deployer.deploy(INVALID_SCOPE_APP);
            super.testInvalidScope();
        } finally {
            deployer.undeploy(INVALID_SCOPE_APP);
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
                    .auth().oauth2(getAdminAccessToken(KEYCLOAK_CONTAINER.getAuthServerUrl()))
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
