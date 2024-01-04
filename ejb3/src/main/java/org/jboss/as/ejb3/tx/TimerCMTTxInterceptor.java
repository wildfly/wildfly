/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.tx;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorFactory;

/**
 * CMT interceptor for timer invocations. An exception is thrown if the transaction is rolled back, so the timer
 * service knows to retry the timeout.
 *
 * @author Stuart Douglas
 */
public class TimerCMTTxInterceptor extends CMTTxInterceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new TimerCMTTxInterceptor());

    protected void ourTxRolledBack() {
        throw EjbLogger.ROOT_LOGGER.timerInvocationRolledBack();
    }
}
