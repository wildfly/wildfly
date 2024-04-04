/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.managedbean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class InterceptorBean {

    private final Logger log = Logger.getLogger(InterceptorBean.class);

    private String name;

    @PostConstruct
    public void initializeInterceptor(InvocationContext context) {
        log.trace("Post constructing it");
        name = "#InterceptorBean#";
    }

    @PreDestroy
    public void destroyInterceptor(InvocationContext context) {
        log.trace("Pre destroying it");
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        if (!context.getMethod().getName().equals("echo")) {
            return context.proceed();
        }
        log.trace("-----> Intercepting call to " + context.getMethod().getDeclaringClass() + "." +  context.getMethod().getName() + "()");
        return name + context.proceed();
    }

}
