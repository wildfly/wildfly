/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security;

import jakarta.security.jacc.PolicyContext;

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
