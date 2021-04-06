/*
 * Copyright 2016 Red Hat, Inc.
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
package org.wildfly.extension.messaging.activemq;

import java.util.HashSet;
import java.util.Set;

import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.auth.server.ServerAuthenticationContext;
import org.wildfly.security.evidence.PasswordGuessEvidence;

/**
 * This class implements an {@link ActiveMQSecurityManager} that uses an Elytron {@link SecurityDomain} to authenticate
 * users and perform role checking.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ElytronSecurityManager implements ActiveMQSecurityManager {

    private final SecurityDomain securityDomain;
    private final String defaultUser;
    private final String defaultPassword;

    /**
     * Creates an instance of {@link ElytronSecurityManager} with the specified {@link SecurityDomain}.
     *
     * @param securityDomain a reference to the Elytron {@link SecurityDomain} that will be used to authenticate users.
     */
    public ElytronSecurityManager(final SecurityDomain securityDomain) {
        if (securityDomain == null)
            throw MessagingLogger.ROOT_LOGGER.invalidNullSecurityDomain();
        this.securityDomain = securityDomain;
        this.defaultUser = DefaultCredentials.getUsername();
        this.defaultPassword = DefaultCredentials.getPassword();
    }

    @Override
    public boolean validateUser(String username, String password) {

        if (defaultUser.equals(username) && defaultPassword.equals(password))
            return true;

        return this.authenticate(username, password) != null;
    }

    @Override
    public boolean validateUserAndRole(String username, String password, Set<Role> roles, CheckType checkType) {

        if (defaultUser.equals(username) && defaultPassword.equals(password))
            return true;

        final SecurityIdentity identity = this.authenticate(username, password);
        final Set<String> filteredRoles = new HashSet<>();
        for (Role role : roles) {
            if (checkType.hasRole(role)) {
                String name = role.getName();
                filteredRoles.add(name);
            }
        }
        return identity.getRoles().containsAny(filteredRoles);
    }

    /**
     * Attempt to authenticate and authorize an username with the specified password evidence.
     *
     * @param username the username being authenticated.
     * @param password the password to be verified.
     * @return a reference to the {@link SecurityIdentity} if the user was successfully authenticated and authorized;
     *  {@code null} otherwise.
     */
    private SecurityIdentity authenticate(final String username, final String password) {

        ServerAuthenticationContext context = this.securityDomain.createNewAuthenticationContext();
        PasswordGuessEvidence evidence = null;
        try {
            if (password == null) {
                if (username == null) {
                    if (context.authorizeAnonymous()) {
                        context.succeed();
                        return context.getAuthorizedIdentity();
                    } else {
                        context.fail();
                        return null;
                    }
                } else {
                    // treat a non-null user name with a null password as a auth failure
                    context.fail();
                    return null;
                }
            }

            context.setAuthenticationName(username);
            evidence = new PasswordGuessEvidence(password.toCharArray());
            if (context.verifyEvidence(evidence)) {
                if (context.authorize()) {
                    context.succeed();
                    return context.getAuthorizedIdentity();
                }
                else {
                    context.fail();
                    MessagingLogger.ROOT_LOGGER.failedAuthorization(username);
                }
            } else {
                context.fail();
                MessagingLogger.ROOT_LOGGER.failedAuthentication(username);
            }
        } catch (IllegalArgumentException | IllegalStateException | RealmUnavailableException e) {
            context.fail();
            MessagingLogger.ROOT_LOGGER.failedAuthenticationWithException(e, username, e.getMessage());
        } finally {
            if (evidence != null) {
                evidence.destroy();
            }
        }
        return null;
    }
}