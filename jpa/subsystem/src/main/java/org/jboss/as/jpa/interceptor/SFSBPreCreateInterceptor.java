/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.interceptor;

import org.jboss.as.jpa.container.SFSBCallStack;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Runs early in the SFSB chain, to make sure that SFSB creation operations can inherit extended persistence contexts properly
 * <p/>
 *
 * @author Stuart Douglas
 */
public class SFSBPreCreateInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new SFSBPreCreateInterceptor());

    @Override
    public Object processInvocation(InterceptorContext interceptorContext) throws Exception {
        try {
            // beginSfsbCreation() will setup a "creation time" thread local store for tracking references to extended
            // persistence contexts.
            SFSBCallStack.beginSfsbCreation();
            return interceptorContext.proceed();
        } finally {
            // bean PostCreate event lifecycle has already completed.
            // endSfsbCreation() will clear the thread local knowledge of "creation time" referenced extended
            // persistence contexts.
            SFSBCallStack.endSfsbCreation();
        }
    }

    private SFSBPreCreateInterceptor() {
    }
}
