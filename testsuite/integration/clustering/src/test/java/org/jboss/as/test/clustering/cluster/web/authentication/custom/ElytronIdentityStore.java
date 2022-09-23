/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
