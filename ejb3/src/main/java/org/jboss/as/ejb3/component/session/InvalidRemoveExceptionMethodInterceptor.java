/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.session;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * @author Stuart Douglas
 */
public class InvalidRemoveExceptionMethodInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new InvalidRemoveExceptionMethodInterceptor());

    public InvalidRemoveExceptionMethodInterceptor() {
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        throw EjbLogger.ROOT_LOGGER.illegalCallToEjbHomeRemove();
    }
}
