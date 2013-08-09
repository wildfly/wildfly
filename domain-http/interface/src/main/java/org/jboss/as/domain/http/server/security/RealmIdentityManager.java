/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2013, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.jboss.as.domain.http.server.security;

import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;
import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;
import static org.jboss.as.domain.management.RealmConfigurationConstants.DIGEST_PLAIN_TEXT;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.DigestCredential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.security.idm.X509CertificateCredential;
import io.undertow.util.HexConverter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.RealmCallback;

import org.jboss.as.core.security.SubjectUserInfo;
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

    private static final Charset UTF_8 = Charset.forName("UTF-8");
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

    private boolean plainTextDigest() {
        Map<String, String> mechConfig = securityRealm.getMechanismConfig(AuthMechanism.DIGEST);
        boolean plainTextDigest = true;
        if (mechConfig.containsKey(DIGEST_PLAIN_TEXT)) {
            plainTextDigest = Boolean.parseBoolean(mechConfig.get(DIGEST_PLAIN_TEXT));
        }

        return plainTextDigest;
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
     * This verify method is used to verify both BASIC authentication and DIGEST authentication requests.
     */

    @Override
    public Account verify(String id, Credential credential) {
        if (credential instanceof PasswordCredential) {
            return verify(id, (PasswordCredential) credential);
        } else if (credential instanceof DigestCredential) {
            return verify(id, (DigestCredential) credential);
        }

        throw MESSAGES.invalidCredentialType(credential.getClass().getName());
    }

    private Account verify(String id, PasswordCredential credential) {
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
            ROOT_LOGGER.debug("Failure handling Callback(s) for BASIC authentication.", e);
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

    private Account verify(String id, DigestCredential credential) {
        assertMechanism(AuthMechanism.DIGEST);

        AuthorizingCallbackHandler ach = securityRealm.getAuthorizingCallbackHandler(AuthMechanism.DIGEST);
        Callback[] callbacks = new Callback[3];
        callbacks[0] = new RealmCallback("Realm", credential.getRealm());
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
            ROOT_LOGGER.debug("Failure handling Callback(s) for BASIC authentication.", e);
            return null;
        }

        byte[] ha1;
        if (plainText) {
            MessageDigest digest = null;
            try {
                digest = credential.getAlgorithm().getMessageDigest();

                digest.update(id.getBytes(UTF_8));
                digest.update((byte) ':');
                digest.update(credential.getRealm().getBytes(UTF_8));
                digest.update((byte) ':');
                digest.update(new String(((PasswordCallback) callbacks[2]).getPassword()).getBytes(UTF_8));

                ha1 = HexConverter.convertToHexBytes(digest.digest());
            } catch (NoSuchAlgorithmException e) {
                ROOT_LOGGER.debug("Unexpected authentication failure", e);
                return null;
            } finally {
                digest.reset();
            }
        } else {
            ha1 = ((DigestHashCallback) callbacks[2]).getHexHash().getBytes(UTF_8);
        }

        try {
            if (credential.verifyHA1(ha1)) {
                Principal user = new SimplePrincipal(id);
                Collection<Principal> userCol = Collections.singleton(user);
                SubjectUserInfo supplemental = ach.createSubjectUserInfo(userCol);

                return new RealmIdentityAccount(supplemental.getSubject(), user);
            }
        } catch (IOException e) {
            ROOT_LOGGER.debug("Unexpected authentication failure", e);
        }

        return null;
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
        public boolean isUserInRole(String role) {
            // TODO - Not really used for domains yet.
            return false;
        }

        public Subject getSubject() {
            // TODO may need to map this method to a domain management API to ensure it can be used.
            return subject;
        }

    }

}
