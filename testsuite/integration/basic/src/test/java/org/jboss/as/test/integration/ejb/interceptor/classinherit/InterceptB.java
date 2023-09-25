/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.classinherit;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * @author <a href="mailto:amay@ingenta.com">Andrew May</a>
 */
public class InterceptB {
    @AroundInvoke
    Object audit(InvocationContext ctx) throws Exception {
        String message = (String) ctx.proceed();
        return "InterceptedB: " + message;
    }
}
