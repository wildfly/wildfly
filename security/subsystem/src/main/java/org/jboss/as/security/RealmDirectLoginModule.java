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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jboss.as.domain.management.RealmConfigurationConstants.DIGEST_PLAIN_TEXT;
import static org.jboss.as.domain.management.RealmConfigurationConstants.VERIFY_PASSWORD_CALLBACK_SUPPORTED;
import static org.wildfly.security.password.interfaces.DigestPassword.ALGORITHM_DIGEST_MD5;

import java.io.IOException;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Collection;
import java.util.HashSet;
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

import org.jboss.as.core.security.RealmRole;
import org.jboss.as.core.security.RealmUser;
import org.jboss.as.core.security.SubjectUserInfo;
import org.jboss.as.domain.management.AuthMechanism;
import org.jboss.as.domain.management.AuthorizingCallbackHandler;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.security.SimpleGroup;
import org.jboss.security.auth.callback.ObjectCallback;
import org.jboss.security.auth.spi.UsernamePasswordLoginModule;
import org.wildfly.common.iteration.ByteIterator;
import org.wildfly.security.auth.callback.CredentialCallback;
import org.wildfly.security.auth.callback.EvidenceVerifyCallback;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.security.password.interfaces.DigestPassword;
import org.wildfly.security.sasl.util.UsernamePasswordHashUtil;

/**
 * A login module implementation to interface directly with the security realm.
 * <p/>
 * This login module allows all interactions with the backing store to be delegated to the realm removing the need for any
 * duplicate and synchronized definitions.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RealmDirectLoginModule extends UsernamePasswordLoginModule {

    private static final String DEFAULT_REALM = "ApplicationRealm";
    private static final String REALM_OPTION = "realm";

    private static final String[] ALL_VALID_OPTIONS =
    {
        REALM_OPTION
    };

    private SecurityRealm securityRealm;
    private AuthMechanism chosenMech;
    private ValidationMode validationMode;
    private UsernamePasswordHashUtil hashUtil;
    private AuthorizingCallbackHandler callbackHandler;
    private DigestCredential digestCredential;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        addValidOptions(ALL_VALID_OPTIONS);
        super.initialize(subject, callbackHandler, sharedState, options);

        final String realm = options.containsKey(REALM_OPTION) ? (String) options.get(REALM_OPTION) : DEFAULT_REALM;

        final ServiceController<?> controller = currentServiceContainer().getService(SecurityRealm.ServiceUtil.createServiceName(realm));
        if (controller != null) {
            securityRealm = (SecurityRealm) controller.getValue();
        }
        if (securityRealm == null) {
            throw SecurityLogger.ROOT_LOGGER.realmNotFound(realm);
        }
        Set<AuthMechanism> authMechs = securityRealm.getSupportedAuthenticationMechanisms();

        if (authMechs.contains(AuthMechanism.DIGEST)) {
            chosenMech = AuthMechanism.DIGEST;
        } else if (authMechs.contains(AuthMechanism.PLAIN)) {
            chosenMech = AuthMechanism.PLAIN;
        } else {
            chosenMech = authMechs.iterator().next();
        }

        if (chosenMech == AuthMechanism.DIGEST || chosenMech == AuthMechanism.PLAIN) {
            Map<String, String> mechOpts = securityRealm.getMechanismConfig(chosenMech);

            if (mechOpts.containsKey(VERIFY_PASSWORD_CALLBACK_SUPPORTED) && Boolean.parseBoolean(mechOpts.get(VERIFY_PASSWORD_CALLBACK_SUPPORTED))) {
                // We give this mode priority as even if digest is supported the realm supplied
                // callback handler can handle the conversion comparison itself.
                validationMode = ValidationMode.VALIDATION;
            } else {
                if (chosenMech == AuthMechanism.DIGEST) {
                    if (mechOpts.containsKey(DIGEST_PLAIN_TEXT) && Boolean.parseBoolean(mechOpts.get(DIGEST_PLAIN_TEXT))) {
                        validationMode = ValidationMode.PASSWORD;
                    } else {
                        validationMode = ValidationMode.DIGEST;
                        try {
                            hashUtil = new UsernamePasswordHashUtil();
                        } catch (NoSuchAlgorithmException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                } else {
                    validationMode = ValidationMode.PASSWORD;
                }
            }
        } else {
            validationMode = ValidationMode.NONE;
        }
    }

    @Override
    public boolean login() throws LoginException {
        if ((digestCredential = getDigestCredential()) != null && validationMode == ValidationMode.VALIDATION) {

            /*
             * Override the validation mode to digest as this is the only mode compatible if a DigestCredential is supplied.
             */
            validationMode = ValidationMode.DIGEST;
        }

        return super.login();
    }

    @Override
    protected String createPasswordHash(String username, String password, String digestOption) throws LoginException {
        throw new UnsupportedOperationException();
    }

    /**
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
                CredentialCallback cc = new CredentialCallback(PasswordCredential.class, ALGORITHM_DIGEST_MD5);
                handle(new Callback[]{rcb, ncb, cc});
                PasswordCredential passwordCredential = (PasswordCredential) cc.getCredential();
                DigestPassword digestPassword =  passwordCredential.getPassword(DigestPassword.class);
                password = ByteIterator.ofBytes(digestPassword.getDigest()).hexEncode().drainToString();

                break;
            case PASSWORD:
                PasswordCallback pcb = new PasswordCallback("Password", false);
                handle(new Callback[]{rcb, ncb, pcb});
                password = String.valueOf(pcb.getPassword());

                break;
        }

        return password;
    }

    private void handle(final Callback[] callbacks) throws LoginException {
        try {
            AuthorizingCallbackHandler callbackHandler = getCallbackHandler();
            callbackHandler.handle(callbacks);
        } catch (IOException e) {
            throw SecurityLogger.ROOT_LOGGER.failureCallingSecurityRealm(e.getMessage());
        } catch (UnsupportedCallbackException e) {
            throw SecurityLogger.ROOT_LOGGER.failureCallingSecurityRealm(e.getMessage());
        }
    }

    private AuthorizingCallbackHandler getCallbackHandler() {
        if (callbackHandler == null) {
            callbackHandler = securityRealm.getAuthorizingCallbackHandler(chosenMech);
        }
        return callbackHandler;
    }

    @Override
    protected boolean validatePassword(String inputPassword, String expectedPassword) {
        if (digestCredential != null) {
            return digestCredential.verifyHA1(expectedPassword.getBytes(UTF_8));
        }

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
                EvidenceVerifyCallback evc = new EvidenceVerifyCallback(new PasswordGuessEvidence(inputPassword.toCharArray()));

                try {
                    handle(new Callback[]{rcb, ncb, evc});
                    return evc.isVerified();
                } catch (LoginException e) {
                    return false;
                }
            default:
                return false;
        }
    }

    private DigestCredential getDigestCredential() {
        ObjectCallback oc = new ObjectCallback("Credential:");

        try {
            super.callbackHandler.handle(new Callback[] { oc });
        } catch (IOException | UnsupportedCallbackException e) {
            return null;
        }

        Object credential = oc.getCredential();
        if (credential instanceof DigestCredential) {
            /*
             * This change is an intermediate change to allow the use of a DigestCredential until we are ready to switch to
             * JAAS.
             *
             * However we only wish to accept trusted implementations so perform this final check.
             */
            if (credential.getClass().getName().equals("org.wildfly.extension.undertow.security.DigestCredentialImpl")) {
                return (DigestCredential) credential;
            }
        }

        return null;
    }

    @Override
    protected Group[] getRoleSets() throws LoginException {
        Collection<Principal> principalCol = new HashSet<Principal>();
        principalCol.add(new RealmUser(getUsername()));
        try {
            AuthorizingCallbackHandler callbackHandler = getCallbackHandler();
            SubjectUserInfo sui = callbackHandler.createSubjectUserInfo(principalCol);

            SimpleGroup sg = new SimpleGroup("Roles");

            Set<RealmRole> roles = sui.getSubject().getPrincipals(RealmRole.class);
            for (RealmRole current : roles) {
                sg.addMember(createIdentity(current.getName()));
            }

            return new Group[]{sg};
        } catch (Exception e) {
            throw SecurityLogger.ROOT_LOGGER.failureCallingSecurityRealm(e.getMessage());
        }
    }

    private enum ValidationMode {
        /**
         * A DigestCallback will be used with the realm to obtain the pre-prepared hash of the username, realm, password
         * combination.
         */
        DIGEST,

        /**
         * A PasswordCallback will be used with the realm to obtain the users plain text password.
         */

        PASSWORD,

        /**
         * The realm being delegated to will be passed a ValidatePasswordCallback to allow the realm to validate the password
         * directly.
         */
        VALIDATION,

        /**
         * Password validation is not possible, however the realm will still be used for group loading.
         */
        NONE
    }

    ;

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
