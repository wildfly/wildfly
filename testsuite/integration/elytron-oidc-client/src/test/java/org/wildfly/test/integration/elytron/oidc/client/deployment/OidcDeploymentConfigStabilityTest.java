/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client.deployment;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

/**
 * Tests for the OpenID Connect authentication mechanism for some specific {@code Stability}.
 * @author <a href="mailto:pesilva@redhat.com">Pedro Hos</a>
 */
@ServerSetup({OidcDeploymentConfigStabilityTest.StabilitySetupTask.class, OidcDeploymentConfigBaseTest.KeycloakAndSystemPropertySetup.class, OidcBaseTest.WildFlyServerSetupTask.class})
@RunWith(Arquillian.class)
@RunAsClient
public class OidcDeploymentConfigStabilityTest extends OidcDeploymentConfigBaseTest {

    @Test
    @OperateOnDeployment(OPENID_SCOPE_APP)
    public void testOpenIDScope() throws Exception {
        try{
            deployer.deploy(OPENID_SCOPE_APP);
            super.testOpenIDScope();
        } finally {
            deployer.undeploy(OPENID_SCOPE_APP);
        }
    }

    @Test
    @OperateOnDeployment(SINGLE_SCOPE_APP)
    public void testSingleScope() throws Exception {
        try {
            deployer.deploy(SINGLE_SCOPE_APP);
            super.testSingleScope();
        } finally {
            deployer.undeploy(SINGLE_SCOPE_APP);
        }
    }

    @Test
    @OperateOnDeployment(MULTIPLE_SCOPE_APP)
    public void testMultipleScope() throws Exception {
        try {
            deployer.deploy(MULTIPLE_SCOPE_APP);
            super.testMultipleScope();
        } finally {
            deployer.undeploy(MULTIPLE_SCOPE_APP);
        }
    }

    @Test
    @OperateOnDeployment(INVALID_SCOPE_APP)
    public void testInvalidScope() throws Exception {
        try {
            deployer.deploy(INVALID_SCOPE_APP);
            super.testInvalidScope();
        } finally {
            deployer.undeploy(INVALID_SCOPE_APP);
        }
    }

    @Test
    @OperateOnDeployment(OAUTH2_REQUEST_METHOD_APP)
    public void testOpenIDWithOauth2Request() throws Exception {
        try {
            deployer.deploy(OAUTH2_REQUEST_METHOD_APP);
            super.testOpenIDWithOauth2Request();
        } finally {
            deployer.undeploy(OAUTH2_REQUEST_METHOD_APP);
        }
    }

    @Test
    @OperateOnDeployment(PLAINTEXT_REQUEST_APP)
    public void testOpenIDWithPlainTextRequest() throws Exception {
        try {
            deployer.deploy(PLAINTEXT_REQUEST_APP);
            super.testOpenIDWithPlainTextRequest();
        } finally {
            deployer.undeploy(PLAINTEXT_REQUEST_APP);
        }
    }

    @Test
    @OperateOnDeployment(PLAINTEXT_REQUEST_APP)
    public void testOpenIDWithPlainTextRequestUri() throws Exception {
        try {
            deployer.deploy(PLAINTEXT_REQUEST_URI_APP);
            super.testOpenIDWithPlainTextRequestUri();
        } finally {
            deployer.undeploy(PLAINTEXT_REQUEST_URI_APP);
        }
    }

    @Test
    @OperateOnDeployment(PLAINTEXT_ENCRYPTED_REQUEST_APP)
    public void testOpenIDWithPlainTextEncryptedRequest() throws Exception {
        try {
            deployer.deploy(PLAINTEXT_ENCRYPTED_REQUEST_APP);
            super.testOpenIDWithPlainTextEncryptedRequest();
        } finally {
            deployer.undeploy(PLAINTEXT_ENCRYPTED_REQUEST_APP);
        }
    }

    @Test
    @OperateOnDeployment(PLAINTEXT_ENCRYPTED_REQUEST_URI_APP)
    public void testOpenIDWithPlainTextEncryptedRequestUri() throws Exception {
        try {
            deployer.deploy(PLAINTEXT_ENCRYPTED_REQUEST_URI_APP);
            super.testOpenIDWithPlainTextEncryptedRequestUri();
        } finally {
            deployer.undeploy(PLAINTEXT_ENCRYPTED_REQUEST_URI_APP);
        }
    }

    @Test
    @OperateOnDeployment(RSA_SIGNED_REQUEST_APP)
    public void testOpenIDWithRsaSignedRequest() throws Exception {
        try {
            deployer.deploy(RSA_SIGNED_REQUEST_APP);
            super.testOpenIDWithRsaSignedRequest();
        } finally {
            deployer.undeploy(RSA_SIGNED_REQUEST_APP);
        }
    }

    @Test
    @OperateOnDeployment(RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP)
    public void testOpenIDWithRsaSignedAndEncryptedRequest() throws Exception {
        try {
            deployer.deploy(RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP);
            super.testOpenIDWithRsaSignedAndEncryptedRequest();
        } finally {
            deployer.undeploy(RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP);
        }
    }

    @Test
    @OperateOnDeployment(SIGNED_AND_ENCRYPTED_REQUEST_URI_APP)
    public void testOpenIDWithSignedAndEncryptedRequestUri() throws Exception {
        try {
            deployer.deploy(SIGNED_AND_ENCRYPTED_REQUEST_URI_APP);
            super.testOpenIDWithSignedAndEncryptedRequestUri();
        } finally {
            deployer.undeploy(SIGNED_AND_ENCRYPTED_REQUEST_URI_APP);
        }
    }


    @Test
    @OperateOnDeployment(PS_SIGNED_REQUEST_URI_APP)
    public void testOpenIDWithPsSignedRequestUri() throws Exception {
        try {
            deployer.deploy(PS_SIGNED_REQUEST_URI_APP);
            super.testOpenIDWithPsSignedRequestUri();
        } finally {
            deployer.undeploy(PS_SIGNED_REQUEST_URI_APP);
        }
    }

    @Test
    @OperateOnDeployment(PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP)
    public void testOpenIDWithPsSignedAndRsaEncryptedRequest() throws Exception {
        try {
            deployer.deploy(PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP);
            super.testOpenIDWithPsSignedAndRsaEncryptedRequest();
        } finally {
            deployer.undeploy(PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP);
        }
    }

    @Test
    @OperateOnDeployment(INVALID_SIGNATURE_ALGORITHM_APP)
    public void testOpenIDWithInvalidSigningAlgorithm() throws Exception {
        try {
            deployer.deploy(INVALID_SIGNATURE_ALGORITHM_APP);
            super.testOpenIDWithInvalidSigningAlgorithm();
        } finally {
            deployer.undeploy(INVALID_SIGNATURE_ALGORITHM_APP);
        }
    }

    @Test
    @OperateOnDeployment(MISSING_SECRET_APP)
    public void testOpenIDWithMissingSecretHmacSigningAlgorithm() throws Exception {
        try {
            deployer.deploy(MISSING_SECRET_APP);
            super.testOpenIDWithMissingSecretHmacSigningAlgorithm();
        } finally {
            deployer.undeploy(MISSING_SECRET_APP);
        }
    }

    public static class StabilitySetupTask extends StabilityServerSetupSnapshotRestoreTasks.Preview {
        @Override
        protected void doSetup(ManagementClient managementClient) throws Exception {
            // Write a system property so the model gets stored with a lower stability level.
            // This is to make sure we can reload back to the higher level from the snapshot
            OidcBaseTest.addSystemProperty(managementClient, OidcDeploymentConfigBaseTest.class);
        }
    }


}
