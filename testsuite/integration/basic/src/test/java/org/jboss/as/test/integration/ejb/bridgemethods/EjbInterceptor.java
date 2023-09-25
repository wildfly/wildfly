/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.bridgemethods;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import org.jboss.classfilewriter.AccessFlag;

/**
 * @author Stuart Douglas
 */
public class EjbInterceptor {

    @AroundInvoke
    public Object invoke(final InvocationContext ic) throws Exception {
        if((AccessFlag.BRIDGE & ic.getMethod().getModifiers()) != 0) {
            throw new RuntimeException("Bridge method passed through invocation context");
        }
        ic.setParameters(new Object[]{true});
        return ic.proceed();
    }

}
