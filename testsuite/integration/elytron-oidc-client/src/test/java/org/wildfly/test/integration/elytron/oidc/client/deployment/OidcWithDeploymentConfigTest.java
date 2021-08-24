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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
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

    private static final String[] APP_NAMES = new String[] {PROVIDER_URL_APP, AUTH_SERVER_URL_APP, WRONG_PROVIDER_URL_APP, WRONG_SECRET_APP, MISSING_EXPRESSION_APP};

    @Deployment(name = PROVIDER_URL_APP)
    public static WebArchive createProviderUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, PROVIDER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_JSON_WITH_PROVIDER_URL_FILE, "oidc.json");
    }

    @Deployment(name = AUTH_SERVER_URL_APP)
    public static WebArchive createAuthServerUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, AUTH_SERVER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_JSON_WITH_AUTH_SERVER_URL_FILE, "oidc.json");
    }

    @Deployment(name = WRONG_PROVIDER_URL_APP)
    public static WebArchive createWrongProviderUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, WRONG_PROVIDER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_JSON_WITH_WRONG_PROVIDER_URL_FILE, "oidc.json");
    }

    @Deployment(name = WRONG_SECRET_APP)
    public static WebArchive createWrongSecretDeployment() {
        return ShrinkWrap.create(WebArchive.class, WRONG_SECRET_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_JSON_WITH_WRONG_SECRET_FILE, "oidc.json");
    }

    @Deployment(name = MISSING_EXPRESSION_APP)
    public static WebArchive createMissingExpressionDeployment() {
        return ShrinkWrap.create(WebArchive.class, MISSING_EXPRESSION_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_JSON_WITH_MISSING_EXPRESSION_FILE, "oidc.json");
    }

    @Test
    @OperateOnDeployment(MISSING_EXPRESSION_APP)
    public void testMissingExpression() throws Exception {
        loginToApp(MISSING_EXPRESSION_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, -1, null, false);
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
