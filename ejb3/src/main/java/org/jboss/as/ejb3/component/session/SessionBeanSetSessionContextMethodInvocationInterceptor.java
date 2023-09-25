/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.session;

import jakarta.ejb.SessionBean;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Interceptor that invokes the {@link SessionBean#setSessionContext(jakarta.ejb.SessionContext)} on session beans
 * which implement the {@link SessionBean} interface.
 *
 * @author Stuart Douglas
 */
public class SessionBeanSetSessionContextMethodInvocationInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new SessionBeanSetSessionContextMethodInvocationInterceptor());

    private SessionBeanSetSessionContextMethodInvocationInterceptor() {
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        SessionBeanComponentInstance instance = (SessionBeanComponentInstance) context.getPrivateData(ComponentInstance.class);
        final InvocationType invocationType = context.getPrivateData(InvocationType.class);
        try {
            context.putPrivateData(InvocationType.class, InvocationType.DEPENDENCY_INJECTION);
            ((SessionBean) context.getTarget()).setSessionContext(instance.getEjbContext());
        } finally {
            context.putPrivateData(InvocationType.class, invocationType);
        }
        return context.proceed();
    }
}
