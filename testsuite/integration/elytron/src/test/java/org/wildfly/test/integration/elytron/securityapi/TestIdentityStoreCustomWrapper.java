/*
 * Copyright 2023 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @author <a href="mailto:carodrig@redhat.com">Cameron Rodriguez</a>
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
