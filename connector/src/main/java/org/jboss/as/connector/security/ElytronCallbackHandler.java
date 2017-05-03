/*
 * Copyright 2017 Red Hat, Inc.
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
package org.jboss.as.connector.security;

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;

import java.io.IOException;
import java.io.Serializable;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;

import org.jboss.jca.core.spi.security.Callback;
import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.auth.server.ServerAuthenticationContext;
import org.wildfly.security.authz.RoleMapper;
import org.wildfly.security.authz.Roles;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * An Elytron based {@link CallbackHandler} implementation designed for the JCA security inflow. It uses the information
 * obtained from the {@link javax.security.auth.callback.Callback}s to authenticate and authorize the identity supplied
 * by the resource adapter and inserts the {@link SecurityIdentity} representing the authorized identity in the subject's
 * private credentials set.
 *
 * @author Flavia Rainone
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ElytronCallbackHandler implements CallbackHandler, Serializable {

    private final SecurityDomain securityDomain;

    private final Callback mappings;

    private Subject executionSubject;

    /**
     * Constructor
     * @param securityDomain the Elytron security domain used to establish the caller principal.
     * @param mappings The mappings.
     */
    public ElytronCallbackHandler(final SecurityDomain securityDomain, final Callback mappings) {
        this.securityDomain = securityDomain;
        this.mappings = mappings;
    }

    /**
     * {@inheritDoc}
     */
    public void handle(javax.security.auth.callback.Callback[] callbacks) throws UnsupportedCallbackException, IOException {
        if (SUBSYSTEM_RA_LOGGER.isTraceEnabled())
            SUBSYSTEM_RA_LOGGER.elytronHandlerHandle(Arrays.toString(callbacks));

        // work wrapper calls the callback handler a second time with default callback values after the handler was invoked
        // by the RA. We must check if the execution subject already contains an identity and allow for replacement of the
        // identity with values found in the default callbacks only if the subject has no identity yet or if the identity
        // is the anonymous one.
        if (this.executionSubject != null) {
            final SecurityIdentity subjectIdentity = this.getPrivateCredential(this.executionSubject, SecurityIdentity.class);
            if (subjectIdentity != null && !subjectIdentity.isAnonymous()) {
                return;
            }
        }

        if (callbacks != null && callbacks.length > 0)
        {
            if (this.mappings != null && this.mappings.isMappingRequired())
            {
                callbacks = this.mappings.mapCallbacks(callbacks);
            }

            GroupPrincipalCallback groupPrincipalCallback = null;
            CallerPrincipalCallback callerPrincipalCallback = null;
            PasswordValidationCallback passwordValidationCallback = null;

            for (javax.security.auth.callback.Callback callback : callbacks) {
                if (callback instanceof GroupPrincipalCallback) {
                    groupPrincipalCallback = (GroupPrincipalCallback) callback;
                    if (this.executionSubject == null) {
                        this.executionSubject = groupPrincipalCallback.getSubject();
                    } else if (!this.executionSubject.equals(groupPrincipalCallback.getSubject())) {
                        // TODO merge the contents of the subjects?
                    }
                } else if (callback instanceof CallerPrincipalCallback) {
                    callerPrincipalCallback = (CallerPrincipalCallback) callback;
                    if (this.executionSubject == null) {
                        this.executionSubject = callerPrincipalCallback.getSubject();
                    } else if (!this.executionSubject.equals(callerPrincipalCallback.getSubject())) {
                        // TODO merge the contents of the subjects?
                    }
                } else if (callback instanceof PasswordValidationCallback) {
                    passwordValidationCallback = (PasswordValidationCallback) callback;
                    if (this.executionSubject == null) {
                        this.executionSubject = passwordValidationCallback.getSubject();
                    } else if (!this.executionSubject.equals(passwordValidationCallback.getSubject())) {
                        // TODO merge the contents of the subjects?
                    }
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
            this.handleInternal(callerPrincipalCallback, groupPrincipalCallback, passwordValidationCallback);
        }
    }

    protected void handleInternal(final CallerPrincipalCallback callerPrincipalCallback, final GroupPrincipalCallback groupPrincipalCallback,
                                  final PasswordValidationCallback passwordValidationCallback) throws IOException {

        if(this.executionSubject == null) {
            throw SUBSYSTEM_RA_LOGGER.executionSubjectNotSetInHandler();
        }
        SecurityIdentity identity = this.securityDomain.getAnonymousSecurityIdentity();

        // establish the caller principal using the info from the callback.
        Principal callerPrincipal = null;
        if (callerPrincipalCallback != null) {
            Principal callbackPrincipal = callerPrincipalCallback.getPrincipal();
            callerPrincipal = callbackPrincipal != null ? new NamePrincipal(callbackPrincipal.getName()) :
                    callerPrincipalCallback.getName() != null ? new NamePrincipal(callerPrincipalCallback.getName()) : null;
        }

        // a null principal is the ra contract for requiring the use of the unauthenticated identity - no point in attempting to authenticate.
        if (callerPrincipal != null) {
            // check if we have a username/password pair to authenticate - first try the password validation callback.
            if (passwordValidationCallback != null) {
                final String username = passwordValidationCallback.getUsername();
                final char[] password = passwordValidationCallback.getPassword();
                try {
                    identity = this.authenticate(username, password);
                    // add a password credential to the execution subject and set the successful result in the callback.
                    this.addPrivateCredential(this.executionSubject, new PasswordCredential(username, password));
                    passwordValidationCallback.setResult(true);
                } catch (SecurityException e) {
                    passwordValidationCallback.setResult(false);
                    return;
                }
            } else {
                // identity not established using the callback - check if the execution subject contains a password credential.
                PasswordCredential passwordCredential = this.getPrivateCredential(this.executionSubject, PasswordCredential.class);
                if (passwordCredential != null) {
                    try {
                        identity = this.authenticate(passwordCredential.getUserName(), passwordCredential.getPassword());
                    } catch (SecurityException e) {
                        return;
                    }
                } else {
                    identity = securityDomain.createAdHocIdentity(callerPrincipal);
                }
            }

            // at this point we either have an authenticated identity or an anonymous one. We must now check if the caller principal
            // is different from the identity principal and switch to the caller principal identity if needed.
            if (!callerPrincipal.equals(identity.getPrincipal())) {
                identity = identity.createRunAsIdentity(callerPrincipal.getName());
            }

            // if we have new roles coming from the group callback, set a new mapper in the identity.
            if (groupPrincipalCallback != null) {
                String[] groups = groupPrincipalCallback.getGroups();
                if (groups != null) {
                    Set<String> roles = new HashSet<>(Arrays.asList(groups));
                    // TODO what category should we use here?
                    identity = identity.withRoleMapper(ElytronSecurityIntegration.SECURITY_IDENTITY_ROLE, RoleMapper.constant(Roles.fromSet(roles)));
                }
            }
        }

        // set the authenticated identity as a private credential in the subject.
        this.executionSubject.getPrincipals().add(identity.getPrincipal());
        this.addPrivateCredential(executionSubject, identity);
    }

    /**
     * Authenticate the user with the given credential against the configured Elytron security domain.
     *
     * @param username the user being authenticated.
     * @param credential the credential used as evidence to verify the user's identity.
     * @return the authenticated and authorized {@link SecurityIdentity}.
     * @throws IOException if an error occurs while authenticating the user.
     */
    private SecurityIdentity authenticate(final String username, final char[] credential) throws IOException {
        final ServerAuthenticationContext context = this.securityDomain.createNewAuthenticationContext();
        final PasswordGuessEvidence evidence = new PasswordGuessEvidence(credential != null ? credential : null);
        try {
            context.setAuthenticationName(username);
            if (context.verifyEvidence(evidence)) {
                if (context.authorize()) {
                    context.succeed();
                    return context.getAuthorizedIdentity();
                } else {
                    context.fail();
                    throw new SecurityException("Authorization failed");
                }
            } else {
                context.fail();
                throw new SecurityException("Authentication failed");
            }
        } catch (IllegalArgumentException | IllegalStateException | RealmUnavailableException e) {
            context.fail();
            throw e;
        } finally {
            if (!context.isDone()) {
                context.fail();
            }
            evidence.destroy();
        }
    }

    protected<T> T getPrivateCredential(final Subject subject, final Class<T> credentialClass) {
        T credential = null;
        if (subject != null) {
            Set<T> credentialSet;
            if (!WildFlySecurityManager.isChecking()) {
                credentialSet = subject.getPrivateCredentials(credentialClass);
            } else {
                credentialSet = AccessController.doPrivileged((PrivilegedAction<Set<T>>) () ->
                        subject.getPrivateCredentials(credentialClass));
            }
            if (!credentialSet.isEmpty()) {
                credential = credentialSet.iterator().next();
            }
        }
        return credential;
    }

    /**
     * Add the specified credential to the subject's private credentials set.
     *
     * @param subject the {@link Subject} to add the credential to.
     * @param credential a reference to the credential.
     */
    protected void addPrivateCredential(final Subject subject, final Object credential) {
        if (!WildFlySecurityManager.isChecking()) {
            subject.getPrivateCredentials().add(credential);
        }
        else {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                subject.getPrivateCredentials().add(credential);
                return null;
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ElytronCallbackHandler@").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append("[mappings=").append(mappings);
        sb.append("]");

        return sb.toString();
    }
}
