/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.container.interceptor;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 * A {@link FlowTracker} implementation. There are 2 Jakarta Interceptors used - {@link NonContainerInterceptor} and the class itself
 * ({@link #aroundInvoke(InvocationContext)}).
 *
 * @author Jaikiran Pai
 */
@Stateless
@Interceptors(NonContainerInterceptor.class)
@Remote(FlowTracker.class)
@LocalBean
public class FlowTrackingBean implements FlowTracker {

    private static final Logger logger = Logger.getLogger(FlowTrackingBean.class);

    public static final String CONTEXT_DATA_KEY = "foo-bar";

    @AroundInvoke
    protected Object aroundInvoke(InvocationContext invocationContext) throws Exception {
        logger.trace("@AroundInvoke on bean invoked");
        final String skipInterceptor = (String) invocationContext.getContextData().get(FlowTrackingBean.CONTEXT_DATA_KEY);
        if (skipInterceptor != null && this.getClass().getName().equals(skipInterceptor)) {
            return invocationContext.proceed();
        }
        return this.getClass().getName() + " " + invocationContext.proceed();
    }

    public String echo(final String msg) {
        logger.trace("EJB invoked!!!");
        return msg;
    }

}
