/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.authentication.custom;

import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.evidence.PasswordGuessEvidence;

/**
 * @author Paul Ferraro
 */
@ApplicationScoped
public class ElytronIdentityStore implements IdentityStore {

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (!(credential instanceof UsernamePasswordCredential)) {
            return CredentialValidationResult.INVALID_RESULT;
        }

        UsernamePasswordCredential usernamePassword = (UsernamePasswordCredential) credential;
        try {
            SecurityIdentity identity = SecurityDomain.getCurrent().authenticate(usernamePassword.getCaller(), new PasswordGuessEvidence(usernamePassword.getPassword().getValue()));

            Set<String> groups = new TreeSet<>();
            identity.getRoles().forEach(groups::add);

            return new CredentialValidationResult(identity.getPrincipal().getName(), groups);
        } catch (RealmUnavailableException e) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        } catch (SecurityException e) {
            return CredentialValidationResult.INVALID_RESULT;
        }
    }
}
