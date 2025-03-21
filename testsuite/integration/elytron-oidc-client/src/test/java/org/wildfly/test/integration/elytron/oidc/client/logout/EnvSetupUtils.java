/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client.logout;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.KEYSTORE_FILE_NAME;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.KEYSTORE_CLASSPATH;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.getRealmRepresentation;

import io.restassured.RestAssured;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.wildfly.extension.elytron.oidc.ElytronOidcExtension;
import org.wildfly.security.http.oidc.Oidc;
import org.wildfly.security.jose.util.JsonSerialization;
import org.wildfly.test.integration.elytron.oidc.client.logout.LoginLogoutBasics.LogoutChannelPaths;
import org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration;
import org.wildfly.test.integration.elytron.oidc.client.KeycloakContainer;
import org.jboss.as.test.integration.security.common.Utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

/*  Implementation of classes declared in the @ServerSetup stmt of the root
    test class.
 */
public class EnvSetupUtils {

    public static KeycloakContainer KEYCLOAK_CONTAINER;

    public static final String CLIENT_SECRET = "longerclientsecretthatisstleast256bitslong";
    public static final String TEST_REALM = "WildFly";
    public static final int CLIENT_PORT = TestSuiteEnvironment.getHttpPort();
    public static final String CLIENT_HOST_NAME = TestSuiteEnvironment.getHttpAddress();
    // This name enables the Docker container (running keycloak) to make the
    // local machine's host accessible to keycloak.
    public static final String HOST_TESTCONTAINERS_INTERNAL = "host.testcontainers.internal";

    private static final String LOGOUT_AUTH_SERVER_URL = "logout.auth.server.url";
    private static final String OIDC_REQUEST_OBJECT_SIGNING_KEYSTORE_FILE = "oidc.request.object.signing.keystore.file";
    private static final String SECURE_DEPLOYMENT_ADDRESS = "subsystem="
            + ElytronOidcExtension.SUBSYSTEM_NAME + "/secure-deployment=";


    public static class KeycloakAndSubsystemSetup extends KeycloakSetup {

        private static Map<String, KeycloakConfiguration.ClientAppType> APP_NAMES;
        private static Map<String, LogoutChannelPaths> APP_LOGOUT;
        private static boolean isOidcServerConfig = false;

        public static void setKeycloakClients(Map<String, KeycloakConfiguration.ClientAppType> appNames) {
            APP_NAMES = appNames;
        }

        public static void setLogoutUrlPaths(Map<String, LogoutChannelPaths> appLogout) {
            APP_LOGOUT = appLogout;
        }

        public static void setOidcServerConfig(boolean flag){
            isOidcServerConfig = flag;
        }

        public static ManagementClient mgtClient = null;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            mgtClient = managementClient;
            super.setup(managementClient, containerId);

            RealmRepresentation realm = getRealmRepresentation(TEST_REALM,
                    CLIENT_SECRET, HOST_TESTCONTAINERS_INTERNAL, CLIENT_PORT, APP_NAMES);

            setOidcLogoutUrls(realm, APP_NAMES, APP_LOGOUT);
            sendRealmCreationRequest(realm);

            if (isOidcServerConfig) {
                // oidc attributes set in server config file
                addOidcServerConfig(managementClient);
            }

            ModelControllerClient client = managementClient.getControllerClient();
            ModelNode operation = createOpNode("system-property=" + LOGOUT_AUTH_SERVER_URL, ModelDescriptionConstants.ADD);
            operation.get("value").set(KEYCLOAK_CONTAINER.getAuthServerUrl());
            Utils.applyUpdate(operation, client);

            operation = createOpNode("system-property=" + OIDC_REQUEST_OBJECT_SIGNING_KEYSTORE_FILE, ModelDescriptionConstants.ADD);
            operation.get("value").set(KEYSTORE_CLASSPATH + KEYSTORE_FILE_NAME);
            Utils.applyUpdate(operation, client);

            if (!isOidcServerConfig) {
                // pass oidc.json logout attribute values via system properties
                operation = createOpNode("system-property=" + Constants.LOGOUT_PATH_VALUE_PROP, ModelDescriptionConstants.ADD);
                operation.get("value").set(Constants.LOGOUT_PATH_VALUE);
                Utils.applyUpdate(operation, client);

                operation = createOpNode("system-property=" + Constants.LOGOUT_CALLBACK_PATH_VALUE_PROP, ModelDescriptionConstants.ADD);
                operation.get("value").set(JsonConfigLogoutTest.BACK_CHANNEL_LOGOUT_URL);
                Utils.applyUpdate(operation, client);

                operation = createOpNode("system-property=" + Constants.POST_LOGOUT_PATH_VALUE_PROP, ModelDescriptionConstants.ADD);
                operation.get("value").set(JsonConfigLogoutTest.POST_LOGOUT_PATH_VALUE);
                Utils.applyUpdate(operation, client);

                operation = createOpNode("system-property=" + Constants.CLIENT_SECRET_PROP, ModelDescriptionConstants.ADD);
                operation.get("value").set(CLIENT_SECRET);
                Utils.applyUpdate(operation, client);
            }
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
            ModelNode operation = createOpNode("system-property=" + LOGOUT_AUTH_SERVER_URL, ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, client);

            operation = createOpNode("system-property=" + OIDC_REQUEST_OBJECT_SIGNING_KEYSTORE_FILE, ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, client);
        }

        /* register rpInitiated, backchannel, frontchannel, postLogoutRedirectUris with Keycloak
         */
        public static void setOidcLogoutUrls(RealmRepresentation realm,
                                             Map<String, KeycloakConfiguration.ClientAppType> clientApps,
                                             Map<String, LogoutChannelPaths> appLogout) throws Exception {

            for (ClientRepresentation client : realm.getClients()) {
                KeycloakConfiguration.ClientAppType value = clientApps.get(client.getClientId());
                if (value == KeycloakConfiguration.ClientAppType.OIDC_CLIENT) {
                    List<String> redirectUris = new ArrayList<>(client.getRedirectUris());
                    String redirectUri = redirectUris.get(0);
                    redirectUris.add("*");
                    client.setRedirectUris(redirectUris);

                    int indx = redirectUri.lastIndexOf("/*");
                    String tmpRedirectUri = redirectUri.substring(0, indx);

                    LogoutChannelPaths logoutChannelUrls = appLogout.get(client.getClientId());
                    if (logoutChannelUrls != null) {
                        if (logoutChannelUrls.backChannelPath != null) {
                            KeycloakConfiguration.setFrontChannelLogoutSessionRequired(
                                    client, false);
                            KeycloakConfiguration.setBackchannelLogoutSessionRequired(
                                    client, true);
                            KeycloakConfiguration.setBackchannelLogoutUrl(client,
                                    logoutChannelUrls.backChannelPath);
                        }
                        if (logoutChannelUrls.frontChannelPath != null) {
                            KeycloakConfiguration.setBackchannelLogoutSessionRequired(
                                    client, false);
                            KeycloakConfiguration.setFrontChannelLogoutSessionRequired(
                                    client, true);
                            KeycloakConfiguration.setFrontChannelLogoutUrl(client,
                                    logoutChannelUrls.frontChannelPath);
                        }
                        if (logoutChannelUrls.postLogoutRedirectPaths != null) {
                            List<String> tmpList = new ArrayList<>();
                            for (String redirectPath : logoutChannelUrls.postLogoutRedirectPaths) {
                                if (redirectPath.startsWith("http")) {
                                    tmpList.add(redirectPath);
                                } else {
                                    tmpList.add("http://" + CLIENT_HOST_NAME + ":" + CLIENT_PORT
                                            + "/" + client.getClientId() + redirectPath);
                                }
                            }

                            KeycloakConfiguration.setPostLogoutRedirectUris(client, tmpList);
                        }
                    }
                }
            }
        }

        public static void sendRealmCreationRequest(RealmRepresentation realm) {
            try {
                String adminAccessToken = KeycloakConfiguration.getAdminAccessToken(KEYCLOAK_CONTAINER.getAuthServerUrl());
                assertNotNull(adminAccessToken);
                RestAssured
                        .given()
                        .auth().oauth2(adminAccessToken)
                        .contentType("application/json")
                        .body(JsonSerialization.writeValueAsBytes(realm))
                        .when()
                        .post(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/admin/realms").then()
                        .statusCode(201);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private static void addOidcServerConfig(ManagementClient managementClient) throws Exception {
            ModelControllerClient client = managementClient.getControllerClient();

            for (String app : APP_NAMES.keySet()) {
                ModelNode operation = createOpNode(
                        SECURE_DEPLOYMENT_ADDRESS + app + ".war", ModelDescriptionConstants.ADD);
                operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM);
                operation.get("realm").set(TEST_REALM);
                operation.get("client-id").set(app);
                operation.get("public-client").set(false);
                operation.get("ssl-required").set("EXTERNAL");
                operation.get(Oidc.LOGOUT_PATH).set(Constants.LOGOUT_PATH_VALUE);
                LogoutChannelPaths appLogout = APP_LOGOUT.get(app);
                if (appLogout.backChannelPath != null) {
                    operation.get(Oidc.LOGOUT_CALLBACK_PATH).set(appLogout.backChannelPath);
                }
                if (appLogout.frontChannelPath != null) {
                    operation.get(Oidc.LOGOUT_CALLBACK_PATH).set(appLogout.frontChannelPath);
                }
                if (appLogout.postLogoutRedirectPaths !=null) {
                    operation.get(Oidc.POST_LOGOUT_REDIRECT_URI).set(appLogout.postLogoutRedirectPaths.get(0));
                }

                Utils.applyUpdate(operation, client);

                operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + app + ".war/credential=secret", ModelDescriptionConstants.ADD);
                operation.get("secret").set(CLIENT_SECRET);
                Utils.applyUpdate(operation, client);
            }

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }

    public static class KeycloakSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            assumeTrue("Docker isn't available, OIDC tests will be skipped", AssumeTestGroupUtil.isDockerAvailable());
            // stmt required to enable container to have access to local
            // machine's port.
            org.testcontainers.Testcontainers.exposeHostPorts(8080);
            KEYCLOAK_CONTAINER = new KeycloakContainer();
            KEYCLOAK_CONTAINER.start();
        }

        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (KEYCLOAK_CONTAINER != null) {
                KEYCLOAK_CONTAINER.stop();
            }
        }
    }


    // This class generates all CLI cmds that set the system properties.
    static class WildFlySystemPropertiesSetupTask extends AbstractSystemPropertiesServerSetupTask {
        private static SystemProperty[] sysProps;

        public static void setLogoutSysProps(Map<String, String> map) {
            sysProps = mapToSystemProperties(map);
        }

        protected SystemProperty[] getSystemProperties() {
            return sysProps;
        }
    }

    /*
    Class enables easy configuring of logging messages to server.log.
    It is being maintained for future debugging needs.
   */
    static class WildFlyServerSetupTask extends ManagementServerSetupTask {
        public WildFlyServerSetupTask() {
            super(createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=logging/logger=org.wildfly.security.http.oidc:add()")
                            .add("/subsystem=logging/logger=org.wildfly.security.http.oidc:write-attribute(name=level, value=TRACE)")
                            .add("/subsystem=logging/logger=io.undertow.server:add()")
                            .add("/subsystem=logging/logger=io.undertow.server:write-attribute(name=level, value=TRACE)")
                            .add("/subsystem=logging/logger=io.undertow:add()")
                            .add("/subsystem=logging/logger=io.undertow:write-attribute(name=level, value=TRACE)")
                            .endBatch()
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=logging/logger=org.wildfly.security.http.oidc:remove()")
                            .add("/subsystem=logging/logger=io.undertow.server:remove()")
                            .add("/subsystem=logging/logger=io.undertow:remove()")
                            .endBatch()
                            .build())
                    .build());
        }
    }
}