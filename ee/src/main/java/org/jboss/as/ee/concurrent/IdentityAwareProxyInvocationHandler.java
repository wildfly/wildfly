/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.ee.concurrent;

import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.internal.ContextProxyInvocationHandler;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * A {@link SecurityIdentity} aware {@link ContextProxyInvocationHandler}.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class IdentityAwareProxyInvocationHandler extends ContextProxyInvocationHandler {

    private final transient SecurityIdentity securityIdentity;

    /**
     * @param contextService
     * @param proxiedObject
     * @param executionProperties
     */
    IdentityAwareProxyInvocationHandler(ContextServiceImpl contextService, Object proxiedObject, Map<String, String> executionProperties) {
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
