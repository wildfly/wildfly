/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.http.server.security;

import static org.jboss.as.domain.management.RealmConfigurationConstants.DIGEST_PLAIN_TEXT;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.security.idm.X509CertificateCredential;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.RealmCallback;

import org.jboss.as.controller.security.SubjectUserInfo;
import org.jboss.as.domain.management.AuthMechanism;
import org.jboss.as.domain.management.AuthorizingCallbackHandler;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.sasl.callback.DigestHashCallback;
import org.jboss.sasl.callback.VerifyPasswordCallback;

/**
 * {@link IdentityManager} implementation to wrap the current security realms.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RealmIdentityManager implements IdentityManager {

    private static final ThreadLocal<AuthMechanism> currentMechanism = new ThreadLocal<AuthMechanism>();

    static void setAuthenticationMechanism(final AuthMechanism mechanism) {
        currentMechanism.set(mechanism);
    }

    private final SecurityRealm securityRealm;

    public RealmIdentityManager(final SecurityRealm securityRealm) {
        this.securityRealm = securityRealm;
    }

    @Override
    public Account verify(Account account) {
        return account;
    }

    /*
     * The next pair of methods would typically be used for Digest style authentication.
     */

    @Override
    public Account getAccount(String id) {
        assertMechanism(AuthMechanism.DIGEST);
        AuthorizingCallbackHandler ach = securityRealm.getAuthorizingCallbackHandler(AuthMechanism.DIGEST);
        Callback[] callbacks = new Callback[3];
        callbacks[0] = new RealmCallback("Realm", securityRealm.getName());
        callbacks[1] = new NameCallback("Username", id);
        boolean plainText = plainTextDigest();
        if (plainText) {
            callbacks[2] = new PasswordCallback("Password", false);
        } else {
            callbacks[2] = new DigestHashCallback("Digest");
        }

        try {
            ach.handle(callbacks);
        } catch (Exception e) {
            // TODO - Error reporting.
            return null;
        }

        Principal user = new SimplePrincipal(id);
        Collection<Principal> userCol = Collections.singleton(user);
        SubjectUserInfo supplemental;
        try {
            supplemental = ach.createSubjectUserInfo(userCol);
        } catch (IOException e) {
            return null;
        }
        // TODO - Will modify for roles later, to begin with just get authentication working.
        if (plainText) {
            return new PlainDigestAccount(supplemental.getSubject(), user, ((PasswordCallback) callbacks[2]).getPassword());
        } else {
            return new HashedDigestAccount(supplemental.getSubject(), user, ((DigestHashCallback) callbacks[2]).getHash());
        }
    }

    private boolean plainTextDigest() {
        Map<String, String> mechConfig = securityRealm.getMechanismConfig(AuthMechanism.DIGEST);
        boolean plainTextDigest = true;
        if (mechConfig.containsKey(DIGEST_PLAIN_TEXT)) {
            plainTextDigest = Boolean.parseBoolean(mechConfig.get(DIGEST_PLAIN_TEXT));
        }

        return plainTextDigest;
    }

    @Override
    public char[] getPassword(Account account) {
        if (account instanceof PlainDigestAccount) {
            return ((PlainDigestAccount) account).getPassword();
        }
        //This is impossible, only here for testing if someone messed up a change
        throw new IllegalArgumentException("Account not instanceof 'PlainDigestAccount'.");
    }

    @Override
    public byte[] getHash(Account account) {
        if (account instanceof HashedDigestAccount) {
            return ((HashedDigestAccount) account).getHash();
        }
        //This is impossible, only here for testing if someone messed up a change
        throw new IllegalArgumentException("Account not instanceof 'HashedDigestAccount'.");
    }

    private class SimplePrincipal implements Principal {

        private final String name;

        private SimplePrincipal(final String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

    }

    /*
     * The single method is used for BASIC authentication to perform validation in a single step.
     */

    @Override
    public Account verify(String id, Credential credential) {
        assertMechanism(AuthMechanism.PLAIN);
        if (credential instanceof PasswordCredential == false) {
            return null;
        }

        AuthorizingCallbackHandler ach = securityRealm.getAuthorizingCallbackHandler(AuthMechanism.PLAIN);
        Callback[] callbacks = new Callback[3];
        callbacks[0] = new RealmCallback("Realm", securityRealm.getName());
        callbacks[1] = new NameCallback("Username", id);
        callbacks[2] = new VerifyPasswordCallback(new String(((PasswordCredential) credential).getPassword()));

        try {
            ach.handle(callbacks);
        } catch (Exception e) {
            // TODO - Error reporting.
            return null;
        }

        if (((VerifyPasswordCallback) callbacks[2]).isVerified() == false) {
            return null;
        }

        Principal user = new SimplePrincipal(id);
        Collection<Principal> userCol = Collections.singleton(user);
        SubjectUserInfo supplemental;
        try {
            supplemental = ach.createSubjectUserInfo(userCol);
        } catch (IOException e) {
            return null;
        }

        return new RealmIdentityAccount(supplemental.getSubject(), user);
    }

    /*
     * The final single method is used for Client Cert style authentication only.
     */

    @Override
    public Account verify(Credential credential) {
        assertMechanism(AuthMechanism.CLIENT_CERT);
        if (credential instanceof X509CertificateCredential == false) {
            return null;
        }
        X509CertificateCredential certCred = (X509CertificateCredential) credential;

        AuthorizingCallbackHandler ach = securityRealm.getAuthorizingCallbackHandler(AuthMechanism.CLIENT_CERT);
        Principal user = certCred.getCertificate().getSubjectDN();
        Collection<Principal> userCol = Collections.singleton(user);
        SubjectUserInfo supplemental;
        try {
            supplemental = ach.createSubjectUserInfo(userCol);
        } catch (IOException e) {
            return null;
        }

        return new RealmIdentityAccount(supplemental.getSubject(), user);
    }

    private static void assertMechanism(final AuthMechanism mechanism) {
        if (mechanism != currentMechanism.get()) {
            //This is impossible, only here for testing if someone messed up a change
            throw new IllegalStateException("Unexpected authentication mechanism executing.");
        }
    }

    private class RealmIdentityAccount implements SubjectAccount {

        private final Subject subject;
        private final Principal principal;

        private RealmIdentityAccount(final Subject subject, final Principal principal) {
            this.subject = subject;
            this.principal = principal;
        }

        @Override
        public Principal getPrincipal() {
            return principal;
        }

        @Override
        public boolean isUserInGroup(String group) {
            // TODO - Not really used for domains yet.
            return false;
        }

        public Subject getSubject() {
            // TODO may need to map this method to a domain management API to ensure it can be used.
            return subject;
        }

    }

    private class PlainDigestAccount extends RealmIdentityAccount {

        private final char[] password;

        private PlainDigestAccount(final Subject subject, final Principal principal, final char[] password) {
            super(subject, principal);
            this.password = password;
        }

        private char[] getPassword() {
            return password;
        }

    }

    private class HashedDigestAccount extends RealmIdentityAccount {

        private final byte[] hash;

        private HashedDigestAccount(final Subject subject, final Principal principal, final byte[] hash) {
            super(subject, principal);
            this.hash = hash;
        }

        private byte[] getHash() {
            return hash;
        }
    }
}
