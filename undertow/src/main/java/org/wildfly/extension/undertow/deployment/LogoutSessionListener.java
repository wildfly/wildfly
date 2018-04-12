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

package org.wildfly.extension.undertow.deployment;

import io.undertow.security.api.AuthenticatedSessionManager;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.session.Session;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;
import io.undertow.servlet.spec.HttpSessionImpl;
import org.jboss.security.AuthenticationManager;
import org.wildfly.extension.undertow.security.AccountImpl;
import org.wildfly.security.manager.WildFlySecurityManager;

import javax.security.auth.Subject;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;

/**
 * Undertow session listener that performs logout on session invalidation. The {@code AuthenticationManager} logout
 * takes care of flushing the principal from cache if a security cache is in use.
 *
 *
 * @author Stuart Douglas
 */
class LogoutSessionListener implements HttpSessionListener {

    private final AuthenticationManager manager;

    LogoutSessionListener(AuthenticationManager manager) {
        this.manager = manager;
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
    }
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        if(WildFlySecurityManager.isChecking()) {
            //we don't use doUnchecked here as there is a chance the below method
            //can run user supplied code
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                sessionDestroyedImpl(se);
                return null;
            });
        } else {
            sessionDestroyedImpl(se);
        }
    }

    private void sessionDestroyedImpl(HttpSessionEvent se) {
        //we need to get the current account
        //there are two options here, we can look for the account in the current request
        //or we can look for the account that has been saved in the session
        //for maximum compatibility we do both
        ServletRequestContext src = ServletRequestContext.current();
        Account requestAccount = null;
        if (src != null) {
            SecurityContext securityContext = src.getExchange().getSecurityContext();
            if(securityContext != null) {
                requestAccount = securityContext.getAuthenticatedAccount();
                if (requestAccount != null) {
                    clearAccount(requestAccount);
                }
            }
        }
        if (se.getSession() instanceof HttpSessionImpl) {
            final HttpSessionImpl impl = (HttpSessionImpl) se.getSession();
            Session session = impl.getSession();
            if (session != null) {
                AuthenticatedSessionManager.AuthenticatedSession authenticatedSession = (AuthenticatedSessionManager.AuthenticatedSession) session.getAttribute(CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession");
                if(authenticatedSession != null) {
                    Account sessionAccount = authenticatedSession.getAccount();
                    if (sessionAccount != null && !sessionAccount.equals(requestAccount)) {
                        clearAccount(sessionAccount);
                    }
                }
            }
        }
    }

    private void clearAccount(Account account) {
        Principal principal = (account instanceof AccountImpl) ? ((AccountImpl) account).getOriginalPrincipal() :
                account.getPrincipal();
        if (principal != null) {
            // perform the logout of the principal using the subject currently set in the security context.
            Subject subject = SecurityActions.getSubject();
            this.manager.logout(principal, subject);
        }
    }
}
