/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.realm;

import static org.wildfly.security.password.interfaces.ClearPassword.ALGORITHM_CLEAR;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.wildfly.extension.elytron.Configurable;
import org.wildfly.security.auth.SupportLevel;
import org.wildfly.security.auth.server.CloseableIterator;
import org.wildfly.security.auth.server.ModifiableRealmIdentity;
import org.wildfly.security.auth.server.ModifiableSecurityRealm;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.MapAttributes;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.evidence.Evidence;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.spec.ClearPasswordSpec;
import org.wildfly.security.password.spec.PasswordSpec;

public class CustomModifiableRealmImpl implements ModifiableSecurityRealm, Configurable {

    Map<String, String> userPassword = new HashMap<>();

    @Override
    public RealmIdentity getRealmIdentity(Principal principal) throws RealmUnavailableException {
        String name = principal.getName();
        return new RealmIdentityImpl(principal, userPassword.get(name));
    }

    @Override
    public SupportLevel getCredentialAcquireSupport(Class<? extends Credential> credentialType, String algorithmName)
        throws RealmUnavailableException {
        return SupportLevel.SUPPORTED;
    }

    @Override
    public SupportLevel getEvidenceVerifySupport(Class<? extends Evidence> evidenceType, String algorithmName)
        throws RealmUnavailableException {
        return SupportLevel.UNSUPPORTED;
    }

    @Override
    public void initialize(Map<String, String> configuration) {
        if (configuration.containsKey("throwException")) {
            throw new IllegalStateException("Only test purpose. This exception was thrown on demand.");
        }

        userPassword.putAll(configuration);
    }

    @Override
    public CloseableIterator<ModifiableRealmIdentity> getRealmIdentityIterator() throws RealmUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    private class RealmIdentityImpl implements RealmIdentity {

        private final Principal principal;
        private final String password;

        private RealmIdentityImpl(final Principal principal, final String password) {
            this.principal = principal;
            this.password = password;
        }

        @Override
        public Principal getRealmIdentityPrincipal() {
            return principal;
        }

        @Override
        public SupportLevel getCredentialAcquireSupport(Class<? extends Credential> credentialType, String algorithmName)
            throws RealmUnavailableException {
            return CustomModifiableRealmImpl.this.getCredentialAcquireSupport(credentialType, algorithmName);
        }

        @Override
        public <C extends Credential> C getCredential(Class<C> credentialType) throws RealmUnavailableException {
            return getCredential(credentialType, null);
        }

        @Override
        public <C extends Credential> C getCredential(Class<C> credentialType, final String algorithmName)
            throws RealmUnavailableException {
            if (password == null || (PasswordCredential.class.isAssignableFrom(credentialType) == false)) {
                return null;
            }

            final PasswordFactory passwordFactory;
            final PasswordSpec passwordSpec;

            passwordFactory = getPasswordFactory(ALGORITHM_CLEAR);
            passwordSpec = new ClearPasswordSpec(password.toCharArray());

            try {
                return credentialType.cast(new PasswordCredential(passwordFactory.generatePassword(passwordSpec)));
            } catch (InvalidKeySpecException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public SupportLevel getEvidenceVerifySupport(Class<? extends Evidence> evidenceType, String algorithmName)
            throws RealmUnavailableException {
            return CustomModifiableRealmImpl.this.getEvidenceVerifySupport(evidenceType, algorithmName);
        }

        @Override
        public boolean verifyEvidence(Evidence evidence) throws RealmUnavailableException {
            if (password == null || evidence instanceof PasswordGuessEvidence == false) {
                return false;
            }
            final char[] guess = ((PasswordGuessEvidence) evidence).getGuess();

            final PasswordFactory passwordFactory;
            final PasswordSpec passwordSpec;
            final Password actualPassword;

            passwordFactory = getPasswordFactory(ALGORITHM_CLEAR);
            passwordSpec = new ClearPasswordSpec(password.toCharArray());

            try {
                actualPassword = passwordFactory.generatePassword(passwordSpec);

                return passwordFactory.verify(actualPassword, guess);
            } catch (InvalidKeySpecException | InvalidKeyException | IllegalStateException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public AuthorizationIdentity getAuthorizationIdentity() throws RealmUnavailableException {
            String role = null;
            for (Entry<String, String> entry : userPassword.entrySet()) {
                if (entry.getKey().equals(principal + "_ROLES")) {
                    role = entry.getValue();
                    break;
                }
            }

            if (role == null) {
                return AuthorizationIdentity.EMPTY;
            }

            return AuthorizationIdentity
                .basicIdentity(new MapAttributes(Collections.singletonMap("groups", Arrays.asList(role))));
        }

        @Override
        public boolean exists() throws RealmUnavailableException {
            return password != null;
        }
    }

    private static PasswordFactory getPasswordFactory(final String algorithm) {
        try {
            return PasswordFactory.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
