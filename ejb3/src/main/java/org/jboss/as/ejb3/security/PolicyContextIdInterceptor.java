/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import javax.security.jacc.PolicyContext;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.security.ParametricPrivilegedAction;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class PolicyContextIdInterceptor implements Interceptor {
    private final String policyContextID;

    public PolicyContextIdInterceptor(final String policyContextID) {
        this.policyContextID = policyContextID;
    }

    public Object processInvocation(final InterceptorContext context) throws Exception {
        final String oldId = PolicyContext.getContextID();
        setContextID(policyContextID);
        try {
            return context.proceed();
        } finally {
            setContextID(oldId);
        }
    }

    private static void setContextID(final String contextID) {
        WildFlySecurityManager.doPrivilegedWithParameter(contextID, (ParametricPrivilegedAction<Void, String>) PolicyContextIdInterceptor::doSetContextID);
    }

    private static Void doSetContextID(final String policyContextID) {
        PolicyContext.setContextID(policyContextID);
        return null;
    }
}
