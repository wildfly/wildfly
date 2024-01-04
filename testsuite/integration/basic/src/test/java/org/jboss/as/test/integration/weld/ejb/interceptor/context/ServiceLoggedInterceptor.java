/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.context;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@ServiceLogged
public class ServiceLoggedInterceptor {


    @Resource
    SessionContext sessionContext;

    @PostConstruct
    public void init(InvocationContext context) throws Exception {
        context.proceed();
    }

    @AroundInvoke
    public Object log(InvocationContext ic) throws Exception {
        return ic.proceed();
    }
}
