/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client.deployment;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration;
import org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest;

/**
 * Tests for the OpenID Connect authentication mechanism for default {@code Stability}.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({OidcDeploymentConfigBaseTest.KeycloakAndSystemPropertySetup.class, OidcBaseTest.WildFlyServerSetupTask.class})
public class OidcWithDeploymentConfigTest extends OidcDeploymentConfigBaseTest {

    @ArquillianResource
    protected static Deployer deployer;

    @Test
    @InSequence(1)
    @OperateOnDeployment(PROVIDER_URL_APP)
    public void testWrongPasswordWithProviderUrl() throws Exception {
        deployer.deploy(PROVIDER_URL_APP);
        super.testWrongPasswordWithProviderUrl();
    }

    @Test
    @InSequence(2)
    @OperateOnDeployment(PROVIDER_URL_APP)
    public void testSucessfulAuthenticationWithProviderUrl() throws Exception {
        super.testSucessfulAuthenticationWithProviderUrl();
    }

    @Test
    @InSequence(3)
    @OperateOnDeployment(PROVIDER_URL_APP)
    public void testWrongRoleWithProviderUrl() throws Exception {
        super.testWrongRoleWithProviderUrl();
    }

    @Test
    @InSequence(4)
    @OperateOnDeployment(PROVIDER_URL_APP)
    public void testTokenProvidedBearerOnlyNotSet() throws Exception {
        super.testTokenProvidedBearerOnlyNotSet();
    }

    @Test
    @InSequence(5)
    @OperateOnDeployment(PROVIDER_URL_APP)
    public void testTokenNotProvidedBearerOnlyNotSet() throws Exception {
        super.testTokenNotProvidedBearerOnlyNotSet();
    }

    @Test
    @InSequence(6)
    @OperateOnDeployment(PROVIDER_URL_APP)
    public void testBasicAuthenticationWithoutEnableBasicAuthSetAndWithoutBearerOnlySet() throws Exception {
        try {
            super.testBasicAuthenticationWithoutEnableBasicAuthSetAndWithoutBearerOnlySet();
        } finally {
            deployer.undeploy(PROVIDER_URL_APP);
        }
    }

    @Test
    @InSequence(7)
    @OperateOnDeployment(AUTH_SERVER_URL_APP)
    public void testWrongPasswordWithAuthServerUrl() throws Exception {
        deployer.deploy(AUTH_SERVER_URL_APP);
        super.testWrongPasswordWithAuthServerUrl();
    }

    @Test
    @InSequence(8)
    @OperateOnDeployment(AUTH_SERVER_URL_APP)
    public void testSucessfulAuthenticationWithAuthServerUrl() throws Exception {
        super.testSucessfulAuthenticationWithAuthServerUrl();
    }

    @Test
    @InSequence(9)
    @OperateOnDeployment(AUTH_SERVER_URL_APP)
    public void testWrongRoleWithAuthServerUrl() throws Exception {
        try {
            super.testWrongRoleWithAuthServerUrl();
        } finally {
            deployer.undeploy(AUTH_SERVER_URL_APP);
        }
    }

    @Test
    @OperateOnDeployment(WRONG_PROVIDER_URL_APP)
    public void testWrongProviderUrl() throws Exception {
        try {
            deployer.deploy(WRONG_PROVIDER_URL_APP);
            super.testWrongProviderUrl();
        } finally {
            deployer.undeploy(WRONG_PROVIDER_URL_APP);
        }
    }

    @Test
    @OperateOnDeployment(WRONG_SECRET_APP)
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
    @OperateOnDeployment(BEARER_ONLY_AUTH_SERVER_URL_APP)
    public void testSucessfulBearerOnlyAuthenticationWithAuthServerUrl() throws Exception {
        deployer.deploy(BEARER_ONLY_AUTH_SERVER_URL_APP);
        super.testSucessfulBearerOnlyAuthenticationWithAuthServerUrl();

    }

    @Test
    @InSequence(11)
    @OperateOnDeployment(BEARER_ONLY_AUTH_SERVER_URL_APP)
    public void testNoTokenProvidedWithAuthServerUrl() throws Exception {
        try {
            super.testNoTokenProvidedWithAuthServerUrl();
        } finally {
            deployer.undeploy(BEARER_ONLY_AUTH_SERVER_URL_APP);
        }
    }

    @Test
    @InSequence(12)
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testSucessfulBearerOnlyAuthenticationWithProviderUrl() throws Exception {
        deployer.deploy(BEARER_ONLY_PROVIDER_URL_APP);
        super.testSucessfulBearerOnlyAuthenticationWithProviderUrl();
    }

    @Test
    @InSequence(13)
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testWrongToken() throws Exception {
        super.testWrongToken();
    }

    @Test
    @InSequence(14)
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testInvalidToken() throws Exception {
        super.testInvalidToken();
    }

    @Test
    @InSequence(15)
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testNoTokenProvidedWithProviderUrl() throws Exception {
        super.testNoTokenProvidedWithProviderUrl();
    }

    @Test
    @InSequence(16)
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testValidTokenViaQueryParameter() throws Exception {
        super.testValidTokenViaQueryParameter();
    }

    @Test
    @InSequence(17)
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testWrongTokenViaQueryParameter() throws Exception {
        super.testWrongTokenViaQueryParameter();
    }

    @Test
    @InSequence(18)
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testInvalidTokenViaQueryParameter() throws Exception {
        super.testInvalidTokenViaQueryParameter();
    }

    @Test
    @InSequence(19)
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testBasicAuthenticationWithoutEnableBasicAuthSet() throws Exception {
        super.testBasicAuthenticationWithoutEnableBasicAuthSet();
    }

    @Test
    @InSequence(20)
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testCorsRequestWithoutEnableCors() throws Exception {
        try {
            super.testCorsRequestWithoutEnableCors();
        } finally {
            deployer.undeploy(BEARER_ONLY_PROVIDER_URL_APP);
        }
    }

    @Test
    @InSequence(21)
    @OperateOnDeployment(BASIC_AUTH_PROVIDER_URL_APP)
    public void testValidCredentialsBasicAuthentication() throws Exception {
        deployer.deploy(BASIC_AUTH_PROVIDER_URL_APP);
        super.testValidCredentialsBasicAuthentication();
    }

    @Test
    @InSequence(22)
    @OperateOnDeployment(BASIC_AUTH_PROVIDER_URL_APP)
    public void testInvalidCredentialsBasicAuthentication() throws Exception {
        try {
            super.testInvalidCredentialsBasicAuthentication();
        } finally {
            deployer.undeploy(BASIC_AUTH_PROVIDER_URL_APP);
        }
    }

    @Test
    @InSequence(23)
    @OperateOnDeployment(CORS_PROVIDER_URL_APP)
    public void testCorsRequestWithEnableCors() throws Exception {
        deployer.deploy(CORS_PROVIDER_URL_APP);
        super.testCorsRequestWithEnableCors();
    }

    @Test
    @InSequence(24)
    @OperateOnDeployment(CORS_PROVIDER_URL_APP)
    public void testCorsRequestWithEnableCorsWithWrongToken() throws Exception {
        super.testCorsRequestWithEnableCorsWithWrongToken();
    }

    @Test
    @InSequence(25)
    @OperateOnDeployment(CORS_PROVIDER_URL_APP)
    public void testCorsRequestWithEnableCorsWithInvalidToken() throws Exception {
        super.testCorsRequestWithEnableCorsWithInvalidToken();
    }

    @Test
    @InSequence(26)
    @OperateOnDeployment(CORS_PROVIDER_URL_APP)
    public void testCorsRequestWithEnableCorsWithInvalidOrigin() throws Exception {
        try {
            super.testCorsRequestWithEnableCorsWithInvalidOrigin();
        } finally {
            deployer.undeploy(CORS_PROVIDER_URL_APP);
        }
    }

    @Test
    @InSequence(27)
    @OperateOnDeployment(FORM_WITH_OIDC_EAR_APP)
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
    @OperateOnDeployment(FORM_WITH_OIDC_EAR_APP)
    public void testInvalidFormWithOidcCredentials() throws Exception {
        try {
            deployer.deploy(FORM_WITH_OIDC_EAR_APP);
            super.testInvalidFormWithOidcCredentials();
        } finally {
            deployer.undeploy(FORM_WITH_OIDC_EAR_APP);
        }
    }
}
