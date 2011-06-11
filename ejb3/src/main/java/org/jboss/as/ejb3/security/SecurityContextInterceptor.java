/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.security;

import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import java.security.PrivilegedAction;

import static java.security.AccessController.doPrivileged;

import javax.ejb.EJBAccessException;

/**
 * Establish the security context.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SecurityContextInterceptor implements Interceptor {
    private final PrivilegedAction<Void> pushAction;
    private final PrivilegedAction<Void> popAction;

    public SecurityContextInterceptor(final SimpleSecurityManager securityManager, final String securityDomain, final String runAs, final String runAsPrincipal) {
        this.pushAction = new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                securityManager.push(securityDomain, runAs, runAsPrincipal);
                return null;
            }
        };
        this.popAction = new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                securityManager.pop();
                return null;
            }
        };
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        // TODO - special cases need to be handled where SecurityContext not established or minimal unauthenticated principal context instead.
        doPrivileged(pushAction);
        try {
            return context.proceed();
        } catch (Exception e) {
            // TODO - Remove
            e.printStackTrace();
            // Whatever the failure the call can not proceed.
            throw new EJBAccessException(e.getMessage());
        } finally {
            doPrivileged(popAction);
        }
    }
}
