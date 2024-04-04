/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.managedbean;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;

/**
 * @author John Bailey
 */
public class InterceptorFromParent {

    private final Logger log = Logger.getLogger(InterceptorBean.class);

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        if (!context.getMethod().getName().equals("echo")) {
            return context.proceed();
        }
        return "#InterceptorFromParent#" + context.proceed();
    }

}
