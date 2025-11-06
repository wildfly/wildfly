/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import org.glassfish.concurro.ContextServiceImpl;
import org.glassfish.concurro.internal.ContextProxyInvocationHandler;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * A {@link SecurityIdentity} aware {@link ContextProxyInvocationHandler}.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class ConcurroIdentityAwareProxyInvocationHandler extends ContextProxyInvocationHandler {

    private final transient SecurityIdentity securityIdentity;

    /**
     * @param contextService
     * @param proxiedObject
     * @param executionProperties
     */
    ConcurroIdentityAwareProxyInvocationHandler(ContextServiceImpl contextService, Object proxiedObject, Map<String, String> executionProperties) {
        super(contextService, proxiedObject, executionProperties);
        SecurityDomain securityDomain = SecurityDomain.getCurrent();
        securityIdentity = securityDomain != null ? securityDomain.getCurrentSecurityIdentity() : null;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (securityIdentity != null) {
            try {
                return securityIdentity.runAs((PrivilegedExceptionAction<Object>) (() -> {
                    try {
                        return super.invoke(proxy, method, args);
                    } catch (Throwable e) {
                        throw new WrapperException(e);
                    }
                }));
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                throw cause instanceof WrapperException ? cause.getCause() : cause;
            }
        } else {
            return super.invoke(proxy, method, args);
        }
    }

    private static class WrapperException extends Exception {

        /**
         * @param cause
         */
        WrapperException(Throwable cause) {
            super(cause);
        }

    }


}
