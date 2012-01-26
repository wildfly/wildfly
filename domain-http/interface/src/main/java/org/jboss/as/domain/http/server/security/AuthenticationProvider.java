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

package org.jboss.as.domain.http.server.security;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.jboss.as.domain.management.RealmUser;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.SubjectCallback;
import org.jboss.as.domain.management.SubjectUserInfo;
import org.jboss.as.domain.management.security.DomainCallbackHandler;
import org.jboss.as.domain.management.security.SubjectSupplemental;

/**
 * The AuthenticationProvider to make available the AuthorizingCallbackHandler to the the authenticators.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class AuthenticationProvider {

    private final SecurityRealm securityRealm;

    AuthenticationProvider(final SecurityRealm securityRealm) {
        this.securityRealm = securityRealm;
    }

    AuthorizingCallbackHandler getCallbackHandler() {
        final DomainCallbackHandler callbackHandler = securityRealm.getCallbackHandler();

        return new AuthorizingCallbackHandler() {

            Subject subject;

            @Override
            public Class<Callback>[] getSupportedCallbacks() {
                return callbackHandler.getSupportedCallbacks();
            }

            @Override
            public boolean isReady() {
                return callbackHandler.isReady();
            }

            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                if (contains(SubjectCallback.class, callbackHandler.getSupportedCallbacks())) {
                    Callback[] newCallbacks = new Callback[callbacks.length + 1];
                    System.arraycopy(callbacks, 0, newCallbacks, 0, callbacks.length);
                    SubjectCallback subjectCallBack = new SubjectCallback();
                    newCallbacks[newCallbacks.length] = subjectCallBack;
                    callbackHandler.handle(newCallbacks);
                    subject = subjectCallBack.getSubject();
                } else {
                    callbackHandler.handle(callbacks);
                }
            }

            @Override
            public SubjectUserInfo createSubjectUserInfo(Principal userPrincipal) throws IOException {
                Subject subject = this.subject == null ? new Subject() : this.subject;
                Collection<Principal> allPrincipals = subject.getPrincipals();
                allPrincipals.add(userPrincipal);
                allPrincipals.add(new RealmUser(userPrincipal.getName()));

                SubjectSupplemental subjectSupplemental = securityRealm.getSubjectSupplemental();
                if (subjectSupplemental != null) {
                    subjectSupplemental.supplementSubject(subject);
                }

                return new HttpSubjectUserInfo(subject);
            }
        };

    }

    private static boolean contains(Class clazz, Class<Callback>[] classes) {
        for (Class<Callback> current : classes) {
            if (current.equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    private static class HttpSubjectUserInfo implements SubjectUserInfo {

        private final Subject subject;

        private HttpSubjectUserInfo(Subject subject) {
            this.subject = subject;
        }

        @Override
        public Collection<Principal> getPrincipals() {
            return subject.getPrincipals();
        }

        @Override
        public Subject getSubject() {
            return subject;
        }

    }

}
