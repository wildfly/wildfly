/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client.subsystem;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jose4j.jws.AlgorithmIdentifiers.NONE;
import static org.jose4j.jws.AlgorithmIdentifiers.HMAC_SHA256;
import static org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA256;
import static org.jose4j.jws.AlgorithmIdentifiers.RSA_PSS_USING_SHA256;
import static org.jose4j.jws.AlgorithmIdentifiers.RSA_PSS_USING_SHA512;
import static org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest.CLIENT_SECRET;
import static org.wildfly.security.http.oidc.Oidc.AuthenticationRequestFormat.REQUEST;
import static org.wildfly.security.http.oidc.Oidc.AuthenticationRequestFormat.REQUEST_URI;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.A128CBC_HS256;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.KEYSTORE_PASS;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.KEYSTORE_ALIAS;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.KEYSTORE_CLASSPATH;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.KEYSTORE_FILE_NAME;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.PKCS12_KEYSTORE_TYPE;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.A192CBC_HS384;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.RSA_OAEP;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.RSA_OAEP_256;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.getRealmRepresentation;

import io.restassured.RestAssured;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

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
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.elytron.oidc.ElytronOidcExtension;
import org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration;
import org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest;
import org.wildfly.test.integration.elytron.oidc.client.deployment.OidcWithDeploymentConfigTest;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

/**
 * Tests for the OpenID Connect authentication mechanism.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ OidcWithSubsystemConfigTest.PreviewStabilitySetupTask.class,
        OidcWithSubsystemConfigTest.KeycloakAndSubsystemSetup.class,
        OidcBaseTest.WildFlyServerSetupTask.class})
public class OidcWithSubsystemConfigTest extends OidcBaseTest {

    private static final String SUBSYSTEM_OVERRIDE_APP = "SubsystemOverrideOidcApp";
    private static final String OIDC_JSON_WITH_SUBSYSTEM_OVERRIDE_FILE = "OidcWithSubsystemOverride.json";
    private static final String KEYCLOAK_PROVIDER = "keycloak";
    private static Map<String, KeycloakConfiguration.ClientAppType> APP_NAMES;
    private static final String SECURE_DEPLOYMENT_ADDRESS = "subsystem=" + ElytronOidcExtension.SUBSYSTEM_NAME + "/secure-deployment=";
    private static final String PROVIDER_ADDRESS = "subsystem=" + ElytronOidcExtension.SUBSYSTEM_NAME + "/provider=";
    private static final String REALM_ADDRESS = "subsystem=" + ElytronOidcExtension.SUBSYSTEM_NAME + "/realm=";

    static {
        APP_NAMES = new HashMap<>();
        APP_NAMES.put(PROVIDER_URL_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(AUTH_SERVER_URL_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(WRONG_PROVIDER_URL_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(WRONG_SECRET_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(SUBSYSTEM_OVERRIDE_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(DIRECT_ACCCESS_GRANT_ENABLED_CLIENT, KeycloakConfiguration.ClientAppType.DIRECT_ACCESS_GRANT_OIDC_CLIENT);
        APP_NAMES.put(BEARER_ONLY_AUTH_SERVER_URL_APP, KeycloakConfiguration.ClientAppType.BEARER_ONLY_CLIENT);
        APP_NAMES.put(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ClientAppType.BEARER_ONLY_CLIENT);
        APP_NAMES.put(BASIC_AUTH_PROVIDER_URL_APP, KeycloakConfiguration.ClientAppType.BEARER_ONLY_CLIENT);
        APP_NAMES.put(CORS_PROVIDER_URL_APP, KeycloakConfiguration.ClientAppType.BEARER_ONLY_CLIENT);
        APP_NAMES.put(CORS_CLIENT, KeycloakConfiguration.ClientAppType.CORS_CLIENT);
        APP_NAMES.put(SINGLE_SCOPE_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(MULTIPLE_SCOPE_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(INVALID_SCOPE_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(OPENID_SCOPE_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(OAUTH2_REQUEST_METHOD_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(PLAINTEXT_REQUEST_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(PLAINTEXT_REQUEST_URI_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(PLAINTEXT_ENCRYPTED_REQUEST_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(PLAINTEXT_ENCRYPTED_REQUEST_URI_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(RSA_SIGNED_REQUEST_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(SIGNED_AND_ENCRYPTED_REQUEST_URI_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(PS_SIGNED_REQUEST_URI_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(INVALID_SIGNATURE_ALGORITHM_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(FORM_WITH_OIDC_OIDC_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
    }

    public OidcWithSubsystemConfigTest() {
        super(Stability.PREVIEW);
    }

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

    @Deployment(name = BEARER_ONLY_AUTH_SERVER_URL_APP)
    public static WebArchive createBearerOnlyAuthServerUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, BEARER_ONLY_AUTH_SERVER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = BEARER_ONLY_PROVIDER_URL_APP)
    public static WebArchive createBearerOnlyProviderUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, BEARER_ONLY_PROVIDER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = BASIC_AUTH_PROVIDER_URL_APP)
    public static WebArchive createBasicAuthProviderUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, BASIC_AUTH_PROVIDER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = CORS_PROVIDER_URL_APP)
    public static WebArchive createCorsProviderUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, CORS_PROVIDER_URL_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = SINGLE_SCOPE_APP)
    public static WebArchive createSingleScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, SINGLE_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class);
    }

    @Deployment(name = MULTIPLE_SCOPE_APP)
    public static WebArchive createMultipleScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, MULTIPLE_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class);
    }

    @Deployment(name = INVALID_SCOPE_APP)
    public static WebArchive createInvalidScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, INVALID_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class);
    }

    @Deployment(name = OPENID_SCOPE_APP)
    public static WebArchive createOpenIdScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, OPENID_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class);
    }

    @Deployment(name = OAUTH2_REQUEST_METHOD_APP)
    public static WebArchive createOpenIDWithOAuth2Request() {
        return ShrinkWrap.create(WebArchive.class, OAUTH2_REQUEST_METHOD_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = PLAINTEXT_REQUEST_APP)
    public static WebArchive createOpenIDWithPLainTextRequest() {
        return ShrinkWrap.create(WebArchive.class, PLAINTEXT_REQUEST_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }
    @Deployment(name = PLAINTEXT_REQUEST_URI_APP)
    public static WebArchive createOpenIDWithPLainTextRequestUri() {
        return ShrinkWrap.create(WebArchive.class, PLAINTEXT_REQUEST_URI_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = PLAINTEXT_ENCRYPTED_REQUEST_APP)
    public static WebArchive createOpenIDWithPLainTextEncryptedRequest() {
        return ShrinkWrap.create(WebArchive.class, PLAINTEXT_ENCRYPTED_REQUEST_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = PLAINTEXT_ENCRYPTED_REQUEST_URI_APP)
    public static WebArchive createOpenIDWithPLainTextEncryptedRequestUri() {
        return ShrinkWrap.create(WebArchive.class, PLAINTEXT_ENCRYPTED_REQUEST_URI_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = RSA_SIGNED_REQUEST_APP)
    public static WebArchive createOpenIDWithRsaSignedRequest() {
        return ShrinkWrap.create(WebArchive.class, RSA_SIGNED_REQUEST_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP)
    public static WebArchive createOpenIDWithRsaSignedAndEncryptedRequest() {
        return ShrinkWrap.create(WebArchive.class, RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = SIGNED_AND_ENCRYPTED_REQUEST_URI_APP)
    public static WebArchive createOpenIDWithSignedAndEncryptedRequestUri() {
        return ShrinkWrap.create(WebArchive.class, SIGNED_AND_ENCRYPTED_REQUEST_URI_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP)
    public static WebArchive createOpenIDWithPsSignedAndRsaEncryptedRequest() {
        return ShrinkWrap.create(WebArchive.class, PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = PS_SIGNED_REQUEST_URI_APP)
    public static WebArchive createOpenIDWithPsSignedARequestUri() {
        return ShrinkWrap.create(WebArchive.class, PS_SIGNED_REQUEST_URI_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = INVALID_SIGNATURE_ALGORITHM_APP)
    public static WebArchive createOpenIDWithInvalidSigningAlgorithm() {
        return ShrinkWrap.create(WebArchive.class, INVALID_SIGNATURE_ALGORITHM_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = MISSING_SECRET_APP)
    public static WebArchive createOpenIDWithMissingSecretHmacSigningAlgorithm() {
        return ShrinkWrap.create(WebArchive.class, MISSING_SECRET_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class);
    }

    @Deployment(name = FORM_WITH_OIDC_EAR_APP)
    public static Archive<?> createFormWithOidcDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, FORM_WITH_OIDC_EAR_APP+".ear");
        ear.addAsManifestResource(OidcWithDeploymentConfigTest.class.getPackage(),
                FORM_WITH_OIDC_EAR_APP+"_application.xml", "application.xml");

        final WebArchive form = ShrinkWrap.create(WebArchive.class, "form.war");
        form.addClasses(SimpleServlet.class);
        form.addClasses(SimpleSecuredServlet.class);
        form.addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(),
                FORM_WITH_OIDC_EAR_APP + "_form_web.xml", "web.xml");
        form.addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(),
                FORM_WITH_OIDC_EAR_APP + "_form_jboss-web.xml", "jboss-web.xml");
        form.addAsWebResource(OidcWithDeploymentConfigTest.class.getPackage(),
                FORM_WITH_OIDC_EAR_APP + "_login.jsp", "login.jsp");
        form.addAsWebResource(OidcWithDeploymentConfigTest.class.getPackage(),
                FORM_WITH_OIDC_EAR_APP + "_error.jsp", "error.jsp");

        ear.addAsModule(form);

        final WebArchive oidc = ShrinkWrap.create(WebArchive.class, "oidc.war");
        oidc.addClasses(SimpleServlet.class);
        oidc.addClasses(SimpleSecuredServlet.class);
        oidc.addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(),
                FORM_WITH_OIDC_EAR_APP+"_oidc_web.xml", "web.xml");
        oidc.addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(),
                FORM_WITH_OIDC_EAR_APP+"_oidc_jboss-web.xml", "jboss-web.xml");
        ear.addAsModule(oidc);
        return ear;
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
            operation.get("secret").set(CLIENT_SECRET);
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
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + WRONG_PROVIDER_URL_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(WRONG_PROVIDER_URL_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set("http://fakeauthserver/auth");
            operation.get("ssl-required").set("EXTERNAL");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + WRONG_PROVIDER_URL_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
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
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + BEARER_ONLY_AUTH_SERVER_URL_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("resource").set(BEARER_ONLY_AUTH_SERVER_URL_APP);
            operation.get("public-client").set(false);
            operation.get("realm").set(TEST_REALM);
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("bearer-only").set("true");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + BEARER_ONLY_PROVIDER_URL_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(BEARER_ONLY_PROVIDER_URL_APP);
            operation.get("public-client").set(false);
            operation.get("provider").set(KEYCLOAK_PROVIDER);
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("bearer-only").set("true");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + BASIC_AUTH_PROVIDER_URL_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(DIRECT_ACCCESS_GRANT_ENABLED_CLIENT);
            operation.get("public-client").set(false);
            operation.get("provider").set(KEYCLOAK_PROVIDER);
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("enable-basic-auth").set("true");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + BASIC_AUTH_PROVIDER_URL_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + CORS_PROVIDER_URL_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(CORS_PROVIDER_URL_APP);
            operation.get("public-client").set(false);
            operation.get("provider").set(KEYCLOAK_PROVIDER);
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("bearer-only").set("true");
            operation.get("enable-cors").set("true");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + MULTIPLE_SCOPE_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(MULTIPLE_SCOPE_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile email phone microprofile-jwt");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + MULTIPLE_SCOPE_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + INVALID_SCOPE_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(INVALID_SCOPE_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("INVALID_SCOPE");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + INVALID_SCOPE_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + OPENID_SCOPE_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(OPENID_SCOPE_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("openid");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + OPENID_SCOPE_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);


            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + SINGLE_SCOPE_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(SINGLE_SCOPE_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + SINGLE_SCOPE_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + OAUTH2_REQUEST_METHOD_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(OAUTH2_REQUEST_METHOD_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile email phone");
            operation.get("authentication-request-format").set("oauth2");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + OAUTH2_REQUEST_METHOD_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + PLAINTEXT_REQUEST_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(PLAINTEXT_REQUEST_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile email phone");
            operation.get("authentication-request-format").set(REQUEST.getValue());
            operation.get("request-object-signing-algorithm").set(NONE);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + PLAINTEXT_REQUEST_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + PLAINTEXT_REQUEST_URI_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(PLAINTEXT_REQUEST_URI_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile email phone");
            operation.get("authentication-request-format").set(REQUEST_URI.getValue());
            operation.get("request-object-signing-algorithm").set(NONE);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + PLAINTEXT_REQUEST_URI_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + PLAINTEXT_ENCRYPTED_REQUEST_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(PLAINTEXT_ENCRYPTED_REQUEST_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile email phone");
            operation.get("authentication-request-format").set(REQUEST.getValue());
            operation.get("request-object-signing-algorithm").set(NONE);
            operation.get("request-object-encryption-alg-value").set(RSA_OAEP);
            operation.get("request-object-encryption-enc-value").set(A128CBC_HS256);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + PLAINTEXT_ENCRYPTED_REQUEST_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + PLAINTEXT_ENCRYPTED_REQUEST_URI_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(PLAINTEXT_ENCRYPTED_REQUEST_URI_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile email phone");
            operation.get("authentication-request-format").set(REQUEST_URI.getValue());
            operation.get("request-object-signing-algorithm").set(NONE);
            operation.get("request-object-encryption-alg-value").set(RSA_OAEP);
            operation.get("request-object-encryption-enc-value").set(A128CBC_HS256);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + PLAINTEXT_ENCRYPTED_REQUEST_URI_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + RSA_SIGNED_REQUEST_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(RSA_SIGNED_REQUEST_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile email phone");
            operation.get("authentication-request-format").set(REQUEST.getValue());
            operation.get("request-object-signing-algorithm").set(RSA_USING_SHA256);
            operation.get("request-object-signing-keystore-file").set(KEYSTORE_CLASSPATH + KEYSTORE_FILE_NAME);
            operation.get("request-object-signing-keystore-password").set(KEYSTORE_PASS);
            operation.get("request-object-signing-key-password").set(KEYSTORE_PASS);
            operation.get("request-object-signing-key-alias").set(KEYSTORE_ALIAS);
            operation.get("request-object-signing-keystore-type").set(PKCS12_KEYSTORE_TYPE);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + RSA_SIGNED_REQUEST_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile email phone");
            operation.get("authentication-request-format").set(REQUEST.getValue());
            operation.get("request-object-signing-algorithm").set(RSA_USING_SHA256);
            operation.get("request-object-encryption-alg-value").set(RSA_OAEP_256);
            operation.get("request-object-encryption-enc-value").set(A192CBC_HS384);
            operation.get("request-object-signing-keystore-file").set(KEYSTORE_CLASSPATH + KEYSTORE_FILE_NAME);
            operation.get("request-object-signing-keystore-password").set(KEYSTORE_PASS);
            operation.get("request-object-signing-key-password").set(KEYSTORE_PASS);
            operation.get("request-object-signing-key-alias").set(KEYSTORE_ALIAS);
            operation.get("request-object-signing-keystore-type").set(PKCS12_KEYSTORE_TYPE);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + SIGNED_AND_ENCRYPTED_REQUEST_URI_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(SIGNED_AND_ENCRYPTED_REQUEST_URI_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile email phone");
            operation.get("authentication-request-format").set(REQUEST_URI.getValue());
            operation.get("request-object-signing-algorithm").set(RSA_PSS_USING_SHA512);
            operation.get("request-object-encryption-alg-value").set(RSA_OAEP_256);
            operation.get("request-object-encryption-enc-value").set(A192CBC_HS384);
            operation.get("request-object-signing-keystore-file").set(KEYSTORE_CLASSPATH + KEYSTORE_FILE_NAME);
            operation.get("request-object-signing-keystore-password").set(KEYSTORE_PASS);
            operation.get("request-object-signing-key-password").set(KEYSTORE_PASS);
            operation.get("request-object-signing-key-alias").set(KEYSTORE_ALIAS);
            operation.get("request-object-signing-keystore-type").set(PKCS12_KEYSTORE_TYPE);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + SIGNED_AND_ENCRYPTED_REQUEST_URI_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile email phone");
            operation.get("authentication-request-format").set(REQUEST.getValue());
            operation.get("request-object-signing-algorithm").set(RSA_PSS_USING_SHA256);
            operation.get("request-object-encryption-alg-value").set(RSA_OAEP_256);
            operation.get("request-object-encryption-enc-value").set(A192CBC_HS384);
            operation.get("request-object-signing-keystore-file").set(KEYSTORE_CLASSPATH + KEYSTORE_FILE_NAME);
            operation.get("request-object-signing-keystore-password").set(KEYSTORE_PASS);
            operation.get("request-object-signing-key-password").set(KEYSTORE_PASS);
            operation.get("request-object-signing-key-alias").set(KEYSTORE_ALIAS);
            operation.get("request-object-signing-keystore-type").set(PKCS12_KEYSTORE_TYPE);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + PS_SIGNED_REQUEST_URI_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(PS_SIGNED_REQUEST_URI_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile email phone");
            operation.get("authentication-request-format").set(REQUEST_URI.getValue());
            operation.get("request-object-signing-algorithm").set(RSA_PSS_USING_SHA256);
            operation.get("request-object-signing-keystore-file").set(KEYSTORE_CLASSPATH + KEYSTORE_FILE_NAME);
            operation.get("request-object-signing-keystore-password").set(KEYSTORE_PASS);
            operation.get("request-object-signing-key-password").set(KEYSTORE_PASS);
            operation.get("request-object-signing-key-alias").set(KEYSTORE_ALIAS);
            operation.get("request-object-signing-keystore-type").set(PKCS12_KEYSTORE_TYPE);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + PS_SIGNED_REQUEST_URI_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + INVALID_SIGNATURE_ALGORITHM_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(INVALID_SIGNATURE_ALGORITHM_APP);
            operation.get("public-client").set(false);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile email phone");
            operation.get("authentication-request-format").set(REQUEST.getValue());
            operation.get("request-object-signing-algorithm").set("RSNULL");
            operation.get("request-object-signing-keystore-file").set(KEYSTORE_CLASSPATH + KEYSTORE_FILE_NAME);
            operation.get("request-object-signing-keystore-password").set(KEYSTORE_PASS);
            operation.get("request-object-signing-key-password").set(KEYSTORE_PASS);
            operation.get("request-object-signing-key-alias").set(KEYSTORE_ALIAS);
            operation.get("request-object-signing-keystore-type").set(PKCS12_KEYSTORE_TYPE);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + INVALID_SIGNATURE_ALGORITHM_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + MISSING_SECRET_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(MISSING_SECRET_APP);
            operation.get("public-client").set(true);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM + "/");
            operation.get("ssl-required").set("EXTERNAL");
            operation.get("scope").set("profile email phone");
            operation.get("authentication-request-format").set(REQUEST.getValue());
            operation.get("request-object-signing-algorithm").set(HMAC_SHA256);
            Utils.applyUpdate(operation, client);

            // only config the WAR that is in the EAR
            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + FORM_WITH_OIDC_OIDC_APP + ".war", ModelDescriptionConstants.ADD);
            operation.get("client-id").set(FORM_WITH_OIDC_OIDC_APP);
            operation.get("public-client").set(false);
            operation.get("provider").set(KEYCLOAK_PROVIDER);
            operation.get("ssl-required").set("EXTERNAL");
            Utils.applyUpdate(operation, client);

            operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + FORM_WITH_OIDC_OIDC_APP + ".war/credential=secret", ModelDescriptionConstants.ADD);
            operation.get("secret").set(CLIENT_SECRET);
            Utils.applyUpdate(operation, client);

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelControllerClient client = managementClient.getControllerClient();
            for (String appName : APP_NAMES.keySet()) {
                if (! appName.equals(CORS_CLIENT) && ! appName.equals(DIRECT_ACCCESS_GRANT_ENABLED_CLIENT)) {
                    removeSecureDeployment(client, appName);
                }
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

    public static class PreviewStabilitySetupTask extends StabilityServerSetupSnapshotRestoreTasks.Preview {
        @Override
        protected void doSetup(ManagementClient managementClient) throws Exception {
            // Write a system property so the model gets stored with a lower stability level.
            // This is to make sure we can reload back to the higher level from the snapshot
            OidcBaseTest.addSystemProperty(managementClient, OidcWithSubsystemConfigTest.class);
        }
    }
}
