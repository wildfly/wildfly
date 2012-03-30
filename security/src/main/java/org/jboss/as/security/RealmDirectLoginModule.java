/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security;

import java.io.IOException;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.acl.Group;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.sasl.RealmCallback;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.security.RealmRole;
import org.jboss.as.domain.management.security.RealmUser;
import org.jboss.as.domain.management.security.SecurityRealmRegistry;
import org.jboss.as.domain.management.security.SubjectSupplemental;
import org.jboss.sasl.callback.DigestHashCallback;
import org.jboss.sasl.callback.VerifyPasswordCallback;
import org.jboss.sasl.util.UsernamePasswordHashUtil;
import org.jboss.security.SimpleGroup;
import org.jboss.security.auth.spi.UsernamePasswordLoginModule;

/**
 * A login module implementation to interface directly with the security realm.
 *
 * This login module allows all interactions with the backing store to be delegated to the realm removing the need for any
 * duplicate and synchronized definitions.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RealmDirectLoginModule extends UsernamePasswordLoginModule {

    private static final String DEFAULT_REALM = "ApplicationRealm";
    private static final String REALM_OPTION = "realm";

    private SecurityRealm securityRealm;
    private ValidationMode validationMode;
    private UsernamePasswordHashUtil hashUtil;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        final String realm = options.containsKey(REALM_OPTION) ? (String) options.get(REALM_OPTION) : DEFAULT_REALM;
        super.initialize(subject, callbackHandler, sharedState, options);
        securityRealm = AccessController.doPrivileged(new PrivilegedAction<SecurityRealm>() {
            public SecurityRealm run() {
                return SecurityRealmRegistry.lookup(realm);
            }
        });
        if (securityRealm == null) {
            throw SecurityMessages.MESSAGES.realmNotFound(realm);
        }
        Class[] supportedCallbacks = securityRealm.getCallbackHandler().getSupportedCallbacks();
        if (contains(VerifyPasswordCallback.class, supportedCallbacks)) {
            // We give this mode priority as even if digest is supported the realm supplied
            // callback handler can handle the conversion comparison itself.
            validationMode = ValidationMode.VALIDATION;
        } else if (contains(DigestHashCallback.class, supportedCallbacks)) {
            validationMode = ValidationMode.DIGEST;
            try {
                hashUtil = new UsernamePasswordHashUtil();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        } else if (contains(PasswordCallback.class, supportedCallbacks)) {
            validationMode = ValidationMode.PASSWORD;
        } else {
            throw SecurityMessages.MESSAGES.noPasswordValidationAvailable(realm);
        }
    }

    @Override
    protected String createPasswordHash(String username, String password, String digestOption) throws LoginException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @see org.jboss.security.auth.spi.UsernamePasswordLoginModule#getUsersPassword()
     */
    @Override
    protected String getUsersPassword() throws LoginException {
        if (validationMode == ValidationMode.VALIDATION) {
            return null;
        }

        RealmCallback rcb = new RealmCallback("Realm", securityRealm.getName());
        NameCallback ncb = new NameCallback("User Name", getUsername());

        String password = null;
        switch (validationMode) {
            case DIGEST:
                DigestHashCallback dhc = new DigestHashCallback("Digest");
                handle(new Callback[] { rcb, ncb, dhc });
                password = dhc.getHexHash();

                break;
            case PASSWORD:
                PasswordCallback pcb = new PasswordCallback("Password", false);
                handle(new Callback[] { rcb, ncb, pcb });
                password = String.valueOf(pcb.getPassword());

                break;
        }

        return password;
    }

    private void handle(final Callback[] callbacks) throws LoginException {
        try {
            securityRealm.getCallbackHandler().handle(callbacks);
        } catch (IOException e) {
            throw SecurityMessages.MESSAGES.failureCallingSecurityRealm(e.getMessage());
        } catch (UnsupportedCallbackException e) {
            throw SecurityMessages.MESSAGES.failureCallingSecurityRealm(e.getMessage());
        }
    }

    @Override
    protected boolean validatePassword(String inputPassword, String expectedPassword) {
        switch (validationMode) {
            case DIGEST:
                String inputHashed = hashUtil.generateHashedHexURP(getUsername(), securityRealm.getName(),
                        inputPassword.toCharArray());

                return expectedPassword.equals(inputHashed);
            case PASSWORD:
                return expectedPassword.equals(inputPassword);
            case VALIDATION:
                RealmCallback rcb = new RealmCallback("Realm", securityRealm.getName());
                NameCallback ncb = new NameCallback("User Name", getUsername());
                VerifyPasswordCallback vpc = new VerifyPasswordCallback(inputPassword);

                try {
                    handle(new Callback[] { rcb, ncb, vpc });
                    return vpc.isVerified();
                } catch (LoginException e) {
                    return false;
                }
            default:
                // Should not be reachable.
                return false;
        }
    }

    @Override
    protected Group[] getRoleSets() throws LoginException {
        SubjectSupplemental ss = securityRealm.getSubjectSupplemental();
        if (ss != null) {
            Subject tempSubject = new Subject();
            tempSubject.getPrincipals().add(new RealmUser(getUsername()));
            try {
                ss.supplementSubject(tempSubject);
                SimpleGroup sg = new SimpleGroup("Roles");

                Set<RealmRole> roles = tempSubject.getPrincipals(RealmRole.class);
                for (RealmRole current : roles) {
                    sg.addMember(createIdentity(current.getName()));
                }

                return new Group[] { sg };
            } catch (Exception e) {
                throw SecurityMessages.MESSAGES.failureCallingSecurityRealm(e.getMessage());
            }

        }
        return new Group[0];
    }

    private enum ValidationMode {
        DIGEST, PASSWORD, VALIDATION
    };

    private static boolean contains(Class clazz, Class<Callback>[] classes) {
        for (Class<Callback> current : classes) {
            if (current.equals(clazz)) {
                return true;
            }
        }
        return false;
    }
}
