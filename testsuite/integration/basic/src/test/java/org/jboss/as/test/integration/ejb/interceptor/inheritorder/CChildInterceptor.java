/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inheritorder;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * @author Ondrej Chaloupka
 */
public class CChildInterceptor extends CParentInterceptor {

    // overriding parent interceptor method = won't be invoked
    public Object interceptParent(final InvocationContext context) throws Exception {
        return "CChild-Parent " + context.proceed().toString();
    }

    @AroundInvoke
    public Object interceptChild(final InvocationContext context) throws Exception {
        return "CChild " + context.proceed().toString();
    }
}
