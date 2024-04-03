/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.oidc.subsystem;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.wildfly.test.manual.elytron.oidc.KeycloakConfiguration.ClientAppType.OIDC_SCOPE_CLIENT;
import static org.wildfly.test.manual.elytron.oidc.KeycloakConfiguration.getRealmRepresentation;

import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;
import org.wildfly.extension.elytron.oidc.ElytronOidcExtension;
import org.wildfly.test.manual.elytron.oidc.KeycloakConfiguration;
import org.wildfly.test.manual.elytron.oidc.OidcBaseTest;

/**
 * Tests for the OpenID Connect authentication mechanism.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ OidcWithSubsystemConfigTest.KeycloakAndSubsystemSetup.class })
public class OidcWithSubsystemConfigTest extends OidcBaseTest {

    private static final String KEYCLOAK_PROVIDER = "keycloak";
    private static Map<String, KeycloakConfiguration.ClientAppType> APP_NAMES;
    private static final String SECURE_DEPLOYMENT_ADDRESS = "subsystem=" + ElytronOidcExtension.SUBSYSTEM_NAME + "/secure-deployment=";
    private static final String PROVIDER_ADDRESS = "subsystem=" + ElytronOidcExtension.SUBSYSTEM_NAME + "/provider=";
    private static final String REALM_ADDRESS = "subsystem=" + ElytronOidcExtension.SUBSYSTEM_NAME + "/realm=";

    static {
        APP_NAMES = new HashMap<>();
        APP_NAMES.put(SINGLE_SCOPE_APP, OIDC_SCOPE_CLIENT);
        APP_NAMES.put(MULTIPLE_SCOPE_APP, OIDC_SCOPE_CLIENT);
        APP_NAMES.put(INVALID_SCOPE_APP, OIDC_SCOPE_CLIENT);
        APP_NAMES.put(OPENID_SCOPE_APP, OIDC_SCOPE_CLIENT);
    }

    @Deployment(name = SINGLE_SCOPE_APP)
    @TargetsContainer(CONTAINER)
    public static WebArchive createSingleScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, SINGLE_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class);
    }

    @Deployment(name = MULTIPLE_SCOPE_APP)
    @TargetsContainer(CONTAINER)
    public static WebArchive createMultipleScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, MULTIPLE_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class);
    }

    @Deployment(name = INVALID_SCOPE_APP)
    @TargetsContainer(CONTAINER)
    public static WebArchive createInvalidScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, INVALID_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class);
    }

    @Deployment(name = OPENID_SCOPE_APP)
    @TargetsContainer(CONTAINER)
    public static WebArchive createOpenIdScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, OPENID_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class);
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

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + MULTIPLE_SCOPE_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(MULTIPLE_SCOPE_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile email phone microprofile-jwt");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + MULTIPLE_SCOPE_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set("secret");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + INVALID_SCOPE_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(INVALID_SCOPE_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("INVALID_SCOPE");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + INVALID_SCOPE_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set("secret");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + OPENID_SCOPE_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(OPENID_SCOPE_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("openid");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + OPENID_SCOPE_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set("secret");
            Utils.applyUpdate(operation, client);


            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + SINGLE_SCOPE_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(SINGLE_SCOPE_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + SINGLE_SCOPE_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set("secret");
            Utils.applyUpdate(operation, client);

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelControllerClient client = managementClient.getControllerClient();
            for (String appName : APP_NAMES.keySet()) {
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