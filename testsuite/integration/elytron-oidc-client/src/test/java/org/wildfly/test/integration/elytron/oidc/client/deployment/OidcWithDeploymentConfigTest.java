/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client.deployment;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.KEYSTORE_FILE_NAME;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.KEYSTORE_CLASSPATH;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.getRealmRepresentation;

import io.restassured.RestAssured;
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
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration;
import org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest;
import org.wildfly.test.integration.elytron.oidc.client.subsystem.SimpleServletWithScope;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

/**
 * Tests for the OpenID Connect authentication mechanism.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ OidcWithDeploymentConfigTest.PreviewStabilitySetupTask.class,
        OidcWithDeploymentConfigTest.KeycloakAndSystemPropertySetup.class,
        OidcBaseTest.WildFlyServerSetupTask.class})
public class OidcWithDeploymentConfigTest extends OidcBaseTest {

    private static final String OIDC_PROVIDER_URL = "oidc.provider.url";
    private static final String OIDC_JSON_WITH_PROVIDER_URL_FILE = "OidcWithProviderUrl.json";

    private static final String OIDC_AUTH_SERVER_URL = "oidc.auth.server.url";
    private static final String OIDC_JSON_WITH_AUTH_SERVER_URL_FILE = "OidcWithAuthServerUrl.json";

    private static final String WRONG_OIDC_PROVIDER_URL = "wrong.oidc.provider.url";
    private static final String OIDC_REQUEST_OBJECT_SIGNING_KEYSTORE_FILE = "oidc.request.object.signing.keystore.file";
    private static final String OIDC_JSON_WITH_WRONG_PROVIDER_URL_FILE = "OidcWithWrongProviderUrl.json";

    private static final String OIDC_JSON_WITH_WRONG_SECRET_FILE = "OidcWithWrongSecret.json";

    private static final String MISSING_EXPRESSION_APP = "MissingExpressionOidcApp";
    private static final String OIDC_JSON_WITH_MISSING_EXPRESSION_FILE = "OidcWithMissingExpression.json";

    private static final String BEARER_ONLY_WITH_AUTH_SERVER_URL_FILE = "BearerOnlyWithAuthServerUrl.json";

    private static final String BEARER_ONLY_WITH_PROVIDER_URL_FILE = "BearerOnlyWithProviderUrl.json";
    private static final String BASIC_AUTH_WITH_PROVIDER_URL_FILE = "BasicAuthWithProviderUrl.json";
    private static final String CORS_WITH_PROVIDER_URL_FILE = "CorsWithProviderUrl.json";
    private static final String SINGLE_SCOPE_FILE = "OidcWithSingleScope.json";
    private static final String MULTI_SCOPE_FILE = "OidcWithMultipleScopes.json";
    private static final String INVALID_SCOPE_FILE = "OidcWithInvalidScope.json";
    private static final String OPENID_SCOPE_FILE = "OidcWithOpenIDScope.json";
    private static final String OAUTH2_REQUEST_FILE = "OidcWithOauth2Request.json";
    private static final String PLAIN_TEXT_REQUEST_FILE = "OidcWithPlainTextRequest.json";
    private static final String PLAIN_TEXT_REQUEST_URI_FILE = "OidcWIthPlainTextRequestUri.json";
    private static final String PLAIN_TEXT_ENCRYPTED_REQUEST_FILE = "OidcWithPlainTextEncryptedRequest.json";
    private static final String PLAIN_TEXT_ENCRYPTED_REQUEST_URI_FILE = "OidcWithPlainTextEncryptedRequestUri.json";
    private static final String RSA_SIGNED_REQUEST_FILE = "OidcWIthRsaSignedRequest.json";
    private static final String RSA_SIGNED_AND_ENCRYPTED_REQUEST_FILE = "OidcWithRsaSignedAndEncryptedRequest.json";
    private static final String SIGNED_AND_ENCRYPTED_REQUEST_URI_FILE = "OidcWithSignedAndEncryptedRequestUri.json";
    private static final String PS_SIGNED_RSA_ENCRYPTED_REQUEST_FILE = "OidcWithPsSignedRsaEncryptedRequest.json";
    private static final String PS_SIGNED_REQUEST_URI_FILE = "OidcWithPsSignedRequestUri.json";
    private static final String INVALID_SIGNATURE_ALGORITHM_FILE = "OidcWithInvalidSigningAlgorithm.json";
    private static final String MISSING_SECRET_WITH_HMAC_ALGORITHM_FILE = "MissingSecretWithHmacAlgorithm.json";

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
        APP_NAMES.put(SINGLE_SCOPE_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(MULTIPLE_SCOPE_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(INVALID_SCOPE_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(OPENID_SCOPE_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(OAUTH2_REQUEST_METHOD_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(PLAINTEXT_REQUEST_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(PLAINTEXT_REQUEST_URI_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(PLAINTEXT_ENCRYPTED_REQUEST_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(PLAINTEXT_ENCRYPTED_REQUEST_URI_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(SIGNED_AND_ENCRYPTED_REQUEST_URI_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(RSA_SIGNED_REQUEST_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(PS_SIGNED_REQUEST_URI_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(INVALID_SIGNATURE_ALGORITHM_FILE, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(MISSING_SECRET_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(FORM_WITH_OIDC_OIDC_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
    }

    public OidcWithDeploymentConfigTest() {
        super(Stability.PREVIEW);
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

    @Deployment(name = SINGLE_SCOPE_APP, managed = false, testable = false)
    public static WebArchive createSingleScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, SINGLE_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), SINGLE_SCOPE_FILE, "oidc.json");
    }

    @Deployment(name = MULTIPLE_SCOPE_APP, managed = false, testable = false)
    public static WebArchive createMultipleScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, MULTIPLE_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), MULTI_SCOPE_FILE, "oidc.json");
    }

    @Deployment(name = INVALID_SCOPE_APP, managed = false, testable = false)
    public static WebArchive createInvalidScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, INVALID_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), INVALID_SCOPE_FILE, "oidc.json");
    }

    @Deployment(name = OPENID_SCOPE_APP, managed = false, testable = false)
    public static WebArchive createOpenIdScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, OPENID_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OPENID_SCOPE_FILE, "oidc.json");
    }

    @Deployment(name = OAUTH2_REQUEST_METHOD_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithOAuth2Request() {
        return ShrinkWrap.create(WebArchive.class, OAUTH2_REQUEST_METHOD_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OAUTH2_REQUEST_FILE, "oidc.json");
    }

    @Deployment(name = PLAINTEXT_REQUEST_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithPlainTextRequest() {
        return ShrinkWrap.create(WebArchive.class, PLAINTEXT_REQUEST_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), PLAIN_TEXT_REQUEST_FILE, "oidc.json");
    }


    @Deployment(name = PLAINTEXT_REQUEST_URI_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithPlainTextRequestUri() {
        return ShrinkWrap.create(WebArchive.class, PLAINTEXT_REQUEST_URI_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), PLAIN_TEXT_REQUEST_URI_FILE, "oidc.json");
    }

    @Deployment(name = PLAINTEXT_ENCRYPTED_REQUEST_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithPlainTextEncryptedRequest() {
        return ShrinkWrap.create(WebArchive.class, PLAINTEXT_ENCRYPTED_REQUEST_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), PLAIN_TEXT_ENCRYPTED_REQUEST_FILE, "oidc.json");
    }

    @Deployment(name = PLAINTEXT_ENCRYPTED_REQUEST_URI_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithPlainTextEncryptedRequestUri() {
        return ShrinkWrap.create(WebArchive.class, PLAINTEXT_ENCRYPTED_REQUEST_URI_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), PLAIN_TEXT_ENCRYPTED_REQUEST_URI_FILE, "oidc.json");
    }

    @Deployment(name = RSA_SIGNED_REQUEST_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithRsaSignedRequest() {
        return ShrinkWrap.create(WebArchive.class, RSA_SIGNED_REQUEST_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), RSA_SIGNED_REQUEST_FILE, "oidc.json");
    }

    @Deployment(name = RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithRsaSignedAndEncryptedRequest() {
        return ShrinkWrap.create(WebArchive.class, RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), RSA_SIGNED_AND_ENCRYPTED_REQUEST_FILE, "oidc.json");
    }

    @Deployment(name = SIGNED_AND_ENCRYPTED_REQUEST_URI_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithSignedAndEncryptedRequestUri() {
        return ShrinkWrap.create(WebArchive.class, SIGNED_AND_ENCRYPTED_REQUEST_URI_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), SIGNED_AND_ENCRYPTED_REQUEST_URI_FILE, "oidc.json");
    }

    @Deployment(name = PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithPsSignedAndRsaEncryptedRequest() {
        return ShrinkWrap.create(WebArchive.class, PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), PS_SIGNED_RSA_ENCRYPTED_REQUEST_FILE, "oidc.json");
    }

    @Deployment(name = PS_SIGNED_REQUEST_URI_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithPsSignedARequestUri() {
        return ShrinkWrap.create(WebArchive.class, PS_SIGNED_REQUEST_URI_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), PS_SIGNED_REQUEST_URI_FILE, "oidc.json");
    }

    @Deployment(name = INVALID_SIGNATURE_ALGORITHM_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithInvalidSigningAlgorithm() {
        return ShrinkWrap.create(WebArchive.class, INVALID_SIGNATURE_ALGORITHM_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), INVALID_SIGNATURE_ALGORITHM_FILE, "oidc.json");
    }

    @Deployment(name = MISSING_SECRET_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithMissingSecretHmacSigningAlgorithm() {
        return ShrinkWrap.create(WebArchive.class, MISSING_SECRET_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), MISSING_SECRET_WITH_HMAC_ALGORITHM_FILE, "oidc.json");
    }

    @Deployment(name = FORM_WITH_OIDC_EAR_APP, managed = false, testable = false)
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
        oidc.addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(),
                FORM_WITH_OIDC_EAR_APP+"_oidc_oidc.json", "oidc.json");
        ear.addAsModule(oidc);
        return ear;
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

    @Test
    @InSequence(27)
    public void testFormWithOidc() throws Exception {
        try {
            deployer.deploy(FORM_WITH_OIDC_EAR_APP);
            super.testFormWithOidc();
        } finally {
            deployer.undeploy(FORM_WITH_OIDC_EAR_APP);
        }
    }

    @Test
    @InSequence(28)
    public void testInvalidFormWithOidcCredentials() throws Exception {
        try {
            deployer.deploy(FORM_WITH_OIDC_EAR_APP);
            super.testInvalidFormWithOidcCredentials();
        } finally {
            deployer.undeploy(FORM_WITH_OIDC_EAR_APP);
        }
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
    public void testOpenIDWithOauth2Request() throws Exception {
        try {
            deployer.deploy(OAUTH2_REQUEST_METHOD_APP);
            super.testOpenIDWithOauth2Request();
        } finally {
            deployer.undeploy(OAUTH2_REQUEST_METHOD_APP);
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
    public void testOpenIDWithPlainTextRequest() throws Exception {
        try {
            deployer.deploy(PLAINTEXT_REQUEST_APP);
            super.testOpenIDWithPlainTextRequest();
        } finally {
            deployer.undeploy(PLAINTEXT_REQUEST_APP);
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

    @Test
    public void testOpenIDWithPlainTextRequestUri() throws Exception {
        try {
            deployer.deploy(PLAINTEXT_REQUEST_URI_APP);
            super.testOpenIDWithPlainTextRequestUri();
        } finally {
            deployer.undeploy(PLAINTEXT_REQUEST_URI_APP);
        }
    }

    @Test
    public void testOpenIDWithPlainTextEncryptedRequest() throws Exception {
        try {
            deployer.deploy(PLAINTEXT_ENCRYPTED_REQUEST_APP);
            super.testOpenIDWithPlainTextEncryptedRequest();
        } finally {
            deployer.undeploy(PLAINTEXT_ENCRYPTED_REQUEST_APP);
        }
    }

    @Test
    public void testOpenIDWithPlainTextEncryptedRequestUri() throws Exception {
        try {
            deployer.deploy(PLAINTEXT_ENCRYPTED_REQUEST_URI_APP);
            super.testOpenIDWithPlainTextEncryptedRequestUri();
        } finally {
            deployer.undeploy(PLAINTEXT_ENCRYPTED_REQUEST_URI_APP);
        }
    }

    @Test
    public void testOpenIDWithRsaSignedRequest() throws Exception {
        try {
            deployer.deploy(RSA_SIGNED_REQUEST_APP);
            super.testOpenIDWithRsaSignedRequest();
        } finally {
            deployer.undeploy(RSA_SIGNED_REQUEST_APP);
        }
    }

    @Test
    public void testOpenIDWithRsaSignedAndEncryptedRequest() throws Exception {
        try {
            deployer.deploy(RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP);
            super.testOpenIDWithRsaSignedAndEncryptedRequest();
        } finally {
            deployer.undeploy(RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP);
        }
    }

    @Test
    public void testOpenIDWithSignedAndEncryptedRequestUri() throws Exception {
        try {
            deployer.deploy(SIGNED_AND_ENCRYPTED_REQUEST_URI_APP);
            super.testOpenIDWithSignedAndEncryptedRequestUri();
        } finally {
            deployer.undeploy(SIGNED_AND_ENCRYPTED_REQUEST_URI_APP);
        }
    }


    @Test
    public void testOpenIDWithPsSignedRequestUri() throws Exception {
        try {
            deployer.deploy(PS_SIGNED_REQUEST_URI_APP);
            super.testOpenIDWithPsSignedRequestUri();
        } finally {
            deployer.undeploy(PS_SIGNED_REQUEST_URI_APP);
        }
    }

    @Test
    public void testOpenIDWithPsSignedAndRsaEncryptedRequest() throws Exception {
        try {
            deployer.deploy(PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP);
            super.testOpenIDWithPsSignedAndRsaEncryptedRequest();
        } finally {
            deployer.undeploy(PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP);
        }
    }

    @Test
    public void testOpenIDWithInvalidSigningAlgorithm() throws Exception {
        try {
            deployer.deploy(INVALID_SIGNATURE_ALGORITHM_APP);
            super.testOpenIDWithInvalidSigningAlgorithm();
        } finally {
            deployer.undeploy(INVALID_SIGNATURE_ALGORITHM_APP);
        }
    }

    @Test
    public void testOpenIDWithMissingSecretHmacSigningAlgorithm() throws Exception {
        try {
            deployer.deploy(MISSING_SECRET_APP);
            super.testOpenIDWithMissingSecretHmacSigningAlgorithm();
        } finally {
            deployer.undeploy(MISSING_SECRET_APP);
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

            operation = createOpNode("system-property=" + OIDC_REQUEST_OBJECT_SIGNING_KEYSTORE_FILE, ModelDescriptionConstants.ADD);
            operation.get("value").set(KEYSTORE_CLASSPATH + KEYSTORE_FILE_NAME);
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

            operation = createOpNode("system-property=" + OIDC_REQUEST_OBJECT_SIGNING_KEYSTORE_FILE, ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, client);
        }
    }

    public static class PreviewStabilitySetupTask extends StabilityServerSetupSnapshotRestoreTasks.Preview {
        @Override
        protected void doSetup(ManagementClient managementClient) throws Exception {
            // Write a system property so the model gets stored with a lower stability level.
            // This is to make sure we can reload back to the higher level from the snapshot
            OidcBaseTest.addSystemProperty(managementClient, OidcWithDeploymentConfigTest.class);
        }
    }
}
