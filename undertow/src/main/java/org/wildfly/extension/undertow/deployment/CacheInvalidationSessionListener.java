/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.undertow.deployment;

import io.undertow.security.api.AuthenticatedSessionManager;
import io.undertow.security.idm.Account;
import io.undertow.server.session.Session;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;
import io.undertow.servlet.spec.HttpSessionImpl;
import org.jboss.security.CacheableManager;
import org.wildfly.extension.undertow.security.AccountImpl;
import org.wildfly.security.manager.WildFlySecurityManager;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.security.Principal;
import java.security.PrivilegedAction;

/**
 * Undertow session listener that flushes the authentication cache on session invalidation
 *
 *
 * @author Stuart Douglas
 */
public class CacheInvalidationSessionListener implements HttpSessionListener {

    private final CacheableManager<?, Principal> cm;

    public CacheInvalidationSessionListener(CacheableManager<?, Principal> cm) {
        this.cm = cm;
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        //we need to get the current account
        //there are two options here, we can look for the account in the current request
        //or we can look for the account that has been saved in the session
        //for maximum compatibility we do both
        ServletRequestContext src = ServletRequestContext.current();
        if (src != null) {
            Account account = src.getExchange().getSecurityContext().getAuthenticatedAccount();
            if (account != null) {
                clearAccount(account);
            }
        }
        if (se.getSession() instanceof HttpSessionImpl) {
            final HttpSessionImpl impl = (HttpSessionImpl) se.getSession();
            Session session;
            if (WildFlySecurityManager.isChecking()) {
                session = WildFlySecurityManager.doChecked(new PrivilegedAction<Session>() {
                    @Override
                    public Session run() {
                        return impl.getSession();
                    }
                });
            } else {
                session = impl.getSession();
            }
            if (session != null) {
                AuthenticatedSessionManager.AuthenticatedSession authenticatedSession = (AuthenticatedSessionManager.AuthenticatedSession) session.getAttribute(CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession");
                if(authenticatedSession != null) {
                    clearAccount(authenticatedSession.getAccount());
                }
            }
        }
    }

    private void clearAccount(Account account) {
        if (account instanceof AccountImpl) {
            cm.flushCache(((AccountImpl) account).getOriginalPrincipal());
        }
        if (account != null) {
            cm.flushCache(account.getPrincipal());
        }
    }
}
