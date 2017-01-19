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

    /** Callback mappings */
    private final Callback mappings;

    // TODO initialize the execution subject at the constructor.
    private Subject executionSubject;
    /**
     * Constructor
     * @param mappings The mappings
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

        if (callbacks != null && callbacks.length > 0)
        {
            if (mappings != null)
            {
                callbacks = mappings.mapCallbacks(callbacks);
            }

            GroupPrincipalCallback groupPrincipalCallback = null;
            CallerPrincipalCallback callerPrincipalCallback = null;
            PasswordValidationCallback passwordValidationCallback = null;

            for (javax.security.auth.callback.Callback callback : callbacks) {
                if (callback instanceof GroupPrincipalCallback) {
                    groupPrincipalCallback = (GroupPrincipalCallback) callback;
                    if (executionSubject == null) {
                        executionSubject = groupPrincipalCallback.getSubject();
                    } else if (!executionSubject.equals(groupPrincipalCallback.getSubject())) {
                        // TODO merge the contents of the subjects?
                    }
                } else if (callback instanceof CallerPrincipalCallback) {
                    callerPrincipalCallback = (CallerPrincipalCallback) callback;
                    if (executionSubject == null) {
                        executionSubject = callerPrincipalCallback.getSubject();
                    } else if (!executionSubject.equals(callerPrincipalCallback.getSubject())) {
                        // TODO merge the contents of the subjects?
                    }
                } else if (callback instanceof PasswordValidationCallback) {
                    passwordValidationCallback = (PasswordValidationCallback) callback;
                    if (executionSubject == null) {
                        executionSubject = passwordValidationCallback.getSubject();
                    } else if (!executionSubject.equals(passwordValidationCallback.getSubject())) {
                        // TODO merge the contents of the subjects?
                    }
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
            this.processResults(callerPrincipalCallback, groupPrincipalCallback, passwordValidationCallback);
        }
    }

    protected void processResults(final CallerPrincipalCallback callerPrincipalCallback, final GroupPrincipalCallback groupPrincipalCallback,
                                  final PasswordValidationCallback passwordValidationCallback) throws IOException {//, UnsupportedCallbackException {

        // spec section 16.4.5 - no CallerPrincipalCallback was handled, check the execution subject's principal set.
        SecurityIdentity authenticatedIdentity = this.securityDomain.getAnonymousSecurityIdentity();
        Principal callerPrincipal = null;
        if (callerPrincipalCallback == null) {
            if (executionSubject.getPrincipals().size() == 1) {
                Principal subjectPrincipal = executionSubject.getPrincipals().iterator().next();
                callerPrincipal = new NamePrincipal(subjectPrincipal.getName());
                // TODO apply mapping to the principal if needed
            } else if (!executionSubject.getPrincipals().isEmpty()) {
                // TODO throw exception here (spec violation)?
            }
        } else {
            Principal callbackPrincipal = callerPrincipalCallback.getPrincipal();
            callerPrincipal = callbackPrincipal != null ? new NamePrincipal(callbackPrincipal.getName()) :
                    callerPrincipalCallback.getName() != null ? new NamePrincipal(callerPrincipalCallback.getName()) : null;
        }

        // a null principal is the ra contract for requiring the use of the unauthenticated identity - there's no point trying to authenticate any identity.
        if (callerPrincipal != null) {
            // check if we have a username/password pair to authenticate - first try the password validation callback.
            if (passwordValidationCallback != null) {
                authenticatedIdentity = this.authenticate(passwordValidationCallback.getUsername(), passwordValidationCallback.getPassword());
            } else {
                // identity not established using the callback - check if the execution subject contains a password credential.
                PasswordCredential passwordCredential = this.getPasswordCredential(this.executionSubject);
                if (passwordCredential != null) {
                    authenticatedIdentity = this.authenticate(passwordCredential.getUserName(), passwordCredential.getPassword());
                }
            }
        }

        // if the caller principal is different from the authenticated identity principal, switch to the caller principal identity.
        if (callerPrincipal != null && !callerPrincipal.equals(authenticatedIdentity.getPrincipal())) {
            authenticatedIdentity = authenticatedIdentity.createRunAsIdentity(callerPrincipal.getName());
        }

         // if we have new roles coming from the group callback, set a new mapper in the identity.
        if (groupPrincipalCallback != null) {
            String[] groups = groupPrincipalCallback.getGroups();
            if (groups != null) {
                Set<String> roles = new HashSet<>(Arrays.asList(groups));
                // TODO what category should we use here? Is it right to assume every entry in the groups array represents a role?
                authenticatedIdentity = authenticatedIdentity.withRoleMapper("jca", RoleMapper.constant(Roles.fromSet(roles)));
            }
        }

        // set the authenticated identity as a private credential in the subject.
        if (executionSubject != null) {
            this.addPrivateCredential(executionSubject, authenticatedIdentity);
        }
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

    protected PasswordCredential getPasswordCredential(final Subject subject) {
        PasswordCredential credential = null;
        if (subject != null) {
            Set<PasswordCredential> credentialSet;
            if (!WildFlySecurityManager.isChecking()) {
                credentialSet = subject.getPrivateCredentials(PasswordCredential.class);
            } else {
                credentialSet = AccessController.doPrivileged((PrivilegedAction<Set<PasswordCredential>>) () ->
                        subject.getPrivateCredentials(PasswordCredential.class));
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
