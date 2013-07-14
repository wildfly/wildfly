/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.concurrent.interceptors;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;

/**
 * An interceptor that restores a previously stored {@link SecurityContext}.
 *
 * @author Eduardo Martins
 */
public class SecurityContextInterceptor implements Interceptor {

    private final SecurityContext securityContext;

    public SecurityContextInterceptor(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final SecurityContext previous = SecurityContextAssociation.getSecurityContext();
        SecurityContextAssociation.setSecurityContext(securityContext);
        try {
            return context.proceed();
        } finally {
            SecurityContextAssociation.setSecurityContext(previous);
        }
    }

    // FACTORY

    private static final InterceptorFactory FACTORY = new InterceptorFactory() {
        @Override
        public Interceptor create(InterceptorFactoryContext context) {
            return new SecurityContextInterceptor(SecurityContextAssociation.getSecurityContext());
        }
    };

    public static InterceptorFactory getFactory() {
        return FACTORY;
    }
}
