/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.securityapi;

import static java.util.Arrays.asList;
import static jakarta.security.enterprise.identitystore.CredentialValidationResult.INVALID_RESULT;

import java.util.HashSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

/**
 * A simple {@link IdentityStore} for testing.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@ApplicationScoped
public class TestIdentityStore implements IdentityStore {

    static final String USERNAME = "user1";
    static final String PASSWORD = "password1";

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (credential instanceof UsernamePasswordCredential) {
            UsernamePasswordCredential upc = (UsernamePasswordCredential) credential;
            if (USERNAME.equals(upc.getCaller()) && PASSWORD.equals(upc.getPasswordAsString())) {
                return new CredentialValidationResult("user1", new HashSet<String>(asList("Role1", "Role2")));
            }
        }

        return INVALID_RESULT;
    }

}
