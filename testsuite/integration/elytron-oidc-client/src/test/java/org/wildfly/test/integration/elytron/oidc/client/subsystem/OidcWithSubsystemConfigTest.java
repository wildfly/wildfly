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

package org.wildfly.test.integration.elytron.oidc.client.subsystem;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.getRealmRepresentation;

import java.net.HttpURLConnection;

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
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.elytron.oidc.ElytronOidcExtension;
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
@ServerSetup({ OidcWithSubsystemConfigTest.KeycloakAndSubsystemSetup.class })
public class OidcWithSubsystemConfigTest extends OidcBaseTest {

    private static final String SUBSYSTEM_OVERRIDE_APP = "SubsystemOverrideOidcApp";
    private static final String OIDC_JSON_WITH_SUBSYSTEM_OVERRIDE_FILE = "OidcWithSubsystemOverride.json";
    private static final String KEYCLOAK_PROVIDER = "keycloak";
    private static final String[] APP_NAMES = new String[] {PROVIDER_URL_APP, AUTH_SERVER_URL_APP, WRONG_PROVIDER_URL_APP, WRONG_SECRET_APP, SUBSYSTEM_OVERRIDE_APP};
    private static final String SECURE_DEPLOYMENT_ADDRESS = "subsystem=" + ElytronOidcExtension.SUBSYSTEM_NAME + "/secure-deployment=";
    private static final String PROVIDER_ADDRESS = "subsystem=" + ElytronOidcExtension.SUBSYSTEM_NAME + "/provider=";
    private static final String REALM_ADDRESS = "subsystem=" + ElytronOidcExtension.SUBSYSTEM_NAME + "/realm=";

    @Deployment(name = PROVIDER_URL_APP)
    public static WebArchive createProviderUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, PROVIDER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = AUTH_SERVER_URL_APP)
    public static WebArchive createAuthServerUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, AUTH_SERVER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = WRONG_PROVIDER_URL_APP)
    public static WebArchive createWrongProviderUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, WRONG_PROVIDER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = WRONG_SECRET_APP)
    public static WebArchive createWrongSecretDeployment() {
        return ShrinkWrap.create(WebArchive.class, WRONG_SECRET_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = SUBSYSTEM_OVERRIDE_APP)
    public static WebArchive createSubsystemOverrideDeployment() {
        return ShrinkWrap.create(WebArchive.class, SUBSYSTEM_OVERRIDE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithSubsystemConfigTest.class.getPackage(), OIDC_JSON_WITH_SUBSYSTEM_OVERRIDE_FILE, "oidc.json"); // has bad provider url
    }

    @Test
    @OperateOnDeployment(SUBSYSTEM_OVERRIDE_APP)
    public void testSubsystemOverride() throws Exception {
        // deployment contains an invalid provider-url but the subsystem contains a valid one, the subsystem config should take precedence
        loginToApp(SUBSYSTEM_OVERRIDE_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY);
    }

    static class KeycloakAndSubsystemSetup extends KeycloakSetup {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            super.setup(managementClient, containerId);
            sendRealmCreationRequest(getRealmRepresentation(TEST_REALM, CLIENT_SECRET, CLIENT_HOST_NAME, CLIENT_PORT, APP_NAMES));

            ModelControllerClient client = managementClient.getControllerClient();
            ModelNode operation = createOpNode(PROVIDER_ADDRESS + KEYCLOAK_PROVIDER , ModelDescriptionConstants.ADD);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + PROVIDER_URL_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(PROVIDER_URL_APP);
            operation.get("public-client").set(false);
            operation.get("provider").set(KEYCLOAK_PROVIDER);
            operation.get("ssl-required").set("EXTERNAL");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + PROVIDER_URL_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set("secret");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(REALM_ADDRESS + TEST_REALM , ModelDescriptionConstants.ADD);
            operation.get("auth-server-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl());
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + AUTH_SERVER_URL_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("resource").set(AUTH_SERVER_URL_APP);
            operation.get("public-client").set(false);
            operation.get("realm").set(TEST_REALM);
            operation.get("ssl-required").set("EXTERNAL");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + AUTH_SERVER_URL_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set("secret");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + WRONG_PROVIDER_URL_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(WRONG_PROVIDER_URL_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set("http://fakeauthserver/auth");
            operation.get("ssl-required").set("EXTERNAL");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + WRONG_PROVIDER_URL_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set("secret");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + WRONG_SECRET_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(WRONG_SECRET_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM);
            operation.get("ssl-required").set("EXTERNAL");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + WRONG_SECRET_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set("WRONG_SECRET");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + SUBSYSTEM_OVERRIDE_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(SUBSYSTEM_OVERRIDE_APP);
            operation.get("public-client").set(false);
            operation.get("provider").set(KEYCLOAK_PROVIDER);
            operation.get("ssl-required").set("EXTERNAL");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + SUBSYSTEM_OVERRIDE_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set("secret");
            Utils.applyUpdate(operation, client);

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelControllerClient client = managementClient.getControllerClient();
            for (String appName : APP_NAMES) {
                removeSecureDeployment(client, appName);
            }

            removeProvider(client, KEYCLOAK_PROVIDER);
            removeRealm(client, TEST_REALM);

            RestAssured
                    .given()
                    .auth().oauth2(KeycloakConfiguration.getAdminAccessToken(KEYCLOAK_CONTAINER.getAuthServerUrl()))
                    .when()
                    .delete(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/admin/realms/" + TEST_REALM).then().statusCode(204);
            super.tearDown(managementClient, containerId);
        }

        private static void removeSecureDeployment(ModelControllerClient client, String name) throws Exception {
            ModelNode operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + name + ".war", ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, client);
        }

        private static void removeProvider(ModelControllerClient client, String provider) throws Exception {
            ModelNode operation = createOpNode(PROVIDER_ADDRESS + provider, ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, client);
        }

        private static void removeRealm(ModelControllerClient client, String realm) throws Exception {
            ModelNode operation = createOpNode(REALM_ADDRESS + realm, ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, client);
        }
    }
}
