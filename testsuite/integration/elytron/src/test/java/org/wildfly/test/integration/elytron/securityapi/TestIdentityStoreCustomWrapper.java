/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.securityapi;

import static jakarta.security.enterprise.identitystore.CredentialValidationResult.Status.VALID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;

/**
 * A thin wrapper for {@link TestIdentityStore} which returns a {@link TestCustomPrincipal custom princpal} in place of
 * a {@link jakarta.security.enterprise.CallerPrincipal}.
 *
 * @author <a href="mailto:jrodri@redhat.com">Jessica Rodriguez</a>
 */
@ApplicationScoped
public class TestIdentityStoreCustomWrapper extends TestIdentityStore {
    @Override
    public CredentialValidationResult validate(Credential credential) {
        CredentialValidationResult cvr = super.validate(credential);
        if (cvr.getStatus() != VALID) return cvr;

        return new CredentialValidationResult(
                new TestCustomPrincipal(cvr.getCallerPrincipal()),
                cvr.getCallerGroups()
        );
    }
}
