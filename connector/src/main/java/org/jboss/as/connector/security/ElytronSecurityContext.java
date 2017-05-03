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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.jca.core.spi.security.SecurityContext;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * An Elytron based {@link SecurityContext} implementation.
 *
 * @author Flavia Rainone
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ElytronSecurityContext implements SecurityContext {

    private Subject authenticatedSubject;

    @Override
    public Subject getAuthenticatedSubject() {
        return this.authenticatedSubject;
    }

    @Override
    public void setAuthenticatedSubject(final Subject subject) {
        this.authenticatedSubject = subject;
    }

    @Override
    public String[] getRoles() {
        if (this.authenticatedSubject != null) {
            // check if the authenticated subject contains a SecurityIdentity in its private credentials.
            Set<SecurityIdentity> authenticatedIdentities = this.getPrivateCredentials(SecurityIdentity.class);
            // iterate through the identities adding all the roles found.
            final Set<String> rolesSet = new HashSet<>();
            for (SecurityIdentity identity : authenticatedIdentities) {
                for (String role : identity.getRoles(ElytronSecurityIntegration.SECURITY_IDENTITY_ROLE)) {
                    rolesSet.add(role);
                }
            }
            return rolesSet.toArray(new String[rolesSet.size()]);
        }
        return new String[0];
    }

    /**
     * Runs the work contained in {@param runnable} as an authenticated Identity.
     *
     * @param work executes the work
     */
    public void runWork(Runnable work) {
        // if we have an authenticated subject we check if it contains a security identity and use the identity to run the work.
        if (this.authenticatedSubject != null) {
            Set<SecurityIdentity> authenticatedIdentities = this.getPrivateCredentials(SecurityIdentity.class);
            if (!authenticatedIdentities.isEmpty()) {
                SecurityIdentity identity = authenticatedIdentities.iterator().next();
                identity.runAs(work);
                return;
            }
        }
        // no authenticated subject found or the subject didn't have a security identity - just run the work.
        work.run();
    }

    protected<T> Set<T> getPrivateCredentials(Class<T> credentialClass) {
        if (!WildFlySecurityManager.isChecking()) {
            return this.authenticatedSubject.getPrivateCredentials(credentialClass);
        } else {
            return AccessController.doPrivileged((PrivilegedAction<Set<T>>) () -> this.authenticatedSubject.getPrivateCredentials(credentialClass));
        }
    }

}
