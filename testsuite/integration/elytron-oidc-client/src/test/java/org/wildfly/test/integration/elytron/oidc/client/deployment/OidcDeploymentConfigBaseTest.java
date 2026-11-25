/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client.deployment;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.KEYSTORE_CLASSPATH;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.KEYSTORE_FILE_NAME;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.getRealmRepresentation;

import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration;
import org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest;
import org.wildfly.test.integration.elytron.oidc.client.subsystem.SimpleServletWithScope;

import io.restassured.RestAssured;

/**
 * Base OIDC Deployment Test used for common test code.
 * @author <a href="mailto:pesilva@redhat.com">Pedro Hos</a>
 */
public abstract class OidcDeploymentConfigBaseTest extends OidcBaseTest {

    protected static final String OIDC_PROVIDER_URL = "oidc.provider.url";
    protected static final String OIDC_AUTH_SERVER_URL = "oidc.auth.server.url";
    protected static final String WRONG_OIDC_PROVIDER_URL = "wrong.oidc.provider.url";
    protected static final String OIDC_REQUEST_OBJECT_SIGNING_KEYSTORE_FILE = "oidc.request.object.signing.keystore.file";
    protected static final String INVALID_SCOPE_FILE = "OidcWithInvalidScope.json";
    protected static final String SINGLE_SCOPE_FILE = "OidcWithSingleScope.json";
    protected static final String MULTI_SCOPE_FILE = "OidcWithMultipleScopes.json";
    protected static final String OPENID_SCOPE_FILE = "OidcWithOpenIDScope.json";
    protected static final String OAUTH2_REQUEST_FILE = "OidcWithOauth2Request.json";
    protected static final String PLAIN_TEXT_REQUEST_FILE = "OidcWithPlainTextRequest.json";
    protected static final String PLAIN_TEXT_REQUEST_URI_FILE = "OidcWIthPlainTextRequestUri.json";
    protected static final String PLAIN_TEXT_ENCRYPTED_REQUEST_FILE = "OidcWithPlainTextEncryptedRequest.json";
    protected static final String PLAIN_TEXT_ENCRYPTED_REQUEST_URI_FILE = "OidcWithPlainTextEncryptedRequestUri.json";
    protected static final String RSA_SIGNED_REQUEST_FILE = "OidcWIthRsaSignedRequest.json";
    protected static final String RSA_SIGNED_AND_ENCRYPTED_REQUEST_FILE = "OidcWithRsaSignedAndEncryptedRequest.json";
    protected static final String SIGNED_AND_ENCRYPTED_REQUEST_URI_FILE = "OidcWithSignedAndEncryptedRequestUri.json";
    protected static final String PS_SIGNED_RSA_ENCRYPTED_REQUEST_FILE = "OidcWithPsSignedRsaEncryptedRequest.json";
    protected static final String PS_SIGNED_REQUEST_URI_FILE = "OidcWithPsSignedRequestUri.json";
    protected static final String MISSING_SECRET_WITH_HMAC_ALGORITHM_FILE = "MissingSecretWithHmacAlgorithm.json";
    protected static final String INVALID_SIGNATURE_ALGORITHM_FILE = "OidcWithInvalidSigningAlgorithm.json";
    protected static final String OIDC_JSON_WITH_PROVIDER_URL_FILE = "OidcWithProviderUrl.json";
    protected static final String OIDC_JSON_WITH_AUTH_SERVER_URL_FILE = "OidcWithAuthServerUrl.json";
    protected static final String OIDC_JSON_WITH_WRONG_PROVIDER_URL_FILE = "OidcWithWrongProviderUrl.json";
    protected static final String OIDC_JSON_WITH_WRONG_SECRET_FILE = "OidcWithWrongSecret.json";
    protected static final String OIDC_JSON_WITH_MISSING_EXPRESSION_FILE = "OidcWithMissingExpression.json";
    protected static final String BEARER_ONLY_WITH_AUTH_SERVER_URL_FILE = "BearerOnlyWithAuthServerUrl.json";
    protected static final String BEARER_ONLY_WITH_PROVIDER_URL_FILE = "BearerOnlyWithProviderUrl.json";
    protected static final String BASIC_AUTH_WITH_PROVIDER_URL_FILE = "BasicAuthWithProviderUrl.json";
    protected static final String CORS_WITH_PROVIDER_URL_FILE = "CorsWithProviderUrl.json";
    protected static final String MISSING_EXPRESSION_APP = "MissingExpressionOidcApp";

    protected static Map<String, KeycloakConfiguration.ClientAppType> APP_NAMES = new HashMap<>();
    static {
        APP_NAMES.put(PROVIDER_URL_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(AUTH_SERVER_URL_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(WRONG_PROVIDER_URL_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(WRONG_SECRET_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(MISSING_EXPRESSION_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(DIRECT_ACCESS_GRANT_ENABLED_CLIENT, KeycloakConfiguration.ClientAppType.DIRECT_ACCESS_GRANT_OIDC_CLIENT);
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

    @ArquillianResource
    protected static Deployer deployer;

    @Deployment(name = INVALID_SCOPE_APP, managed = false, testable = false)
    public static WebArchive createInvalidScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, INVALID_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), INVALID_SCOPE_FILE, "oidc.json");
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

    @Deployment(name = OPENID_SCOPE_APP, managed = false, testable = false)
    public static WebArchive createOpenIdScopeDeployment() {
        return ShrinkWrap.create(WebArchive.class, OPENID_SCOPE_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleServletWithScope.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OPENID_SCOPE_FILE, "oidc.json");
    }

    @Deployment(name = SIGNED_AND_ENCRYPTED_REQUEST_URI_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithSignedAndEncryptedRequestUri() {
        return ShrinkWrap.create(WebArchive.class, SIGNED_AND_ENCRYPTED_REQUEST_URI_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), SIGNED_AND_ENCRYPTED_REQUEST_URI_FILE, "oidc.json");
    }

    @Deployment(name = RSA_SIGNED_REQUEST_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithRsaSignedRequest() {
        return ShrinkWrap.create(WebArchive.class, RSA_SIGNED_REQUEST_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), RSA_SIGNED_REQUEST_FILE, "oidc.json");
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

    @Deployment(name = RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP, managed = false, testable = false)
    public static WebArchive createOpenIDWithRsaSignedAndEncryptedRequest() {
        return ShrinkWrap.create(WebArchive.class, RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML, "web.xml")
                .addAsWebInfResource(OidcWithDeploymentConfigTest.class.getPackage(), RSA_SIGNED_AND_ENCRYPTED_REQUEST_FILE, "oidc.json");
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
                    .auth().oauth2(KeycloakConfiguration.getAdminAccessToken(KEYCLOAK_CONTAINER.getAuthServerUrl()))
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
}
