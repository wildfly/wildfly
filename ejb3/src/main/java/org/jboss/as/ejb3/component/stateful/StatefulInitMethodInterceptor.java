/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.stateful;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jakarta.ejb.CreateException;

import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.interceptors.EjbExceptionTransformingInterceptorFactories;
import org.jboss.as.ejb3.component.interceptors.SessionBeanHomeInterceptorFactory;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;

/**
 * Interceptor factory for SFSB's that invokes the ejbCreate method. This interceptor
 * is only used when a component is created via a home interface method.
 *
 * @author Stuart Douglas
 */
public class StatefulInitMethodInterceptor implements Interceptor {


    public static final InterceptorFactory INSTANCE = new ImmediateInterceptorFactory(new StatefulInitMethodInterceptor());

    private StatefulInitMethodInterceptor() {

    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final Method method = SessionBeanHomeInterceptorFactory.INIT_METHOD.get();
        final Object[] params = SessionBeanHomeInterceptorFactory.INIT_PARAMETERS.get();
        //we remove them immediately, so they are not set for the rest of the invocation
        //TODO: find a better way to handle this
        SessionBeanHomeInterceptorFactory.INIT_METHOD.remove();
        SessionBeanHomeInterceptorFactory.INIT_PARAMETERS.remove();
        if (method != null) {
            final InvocationType invocationType = context.getPrivateData(InvocationType.class);
            try {
                context.putPrivateData(InvocationType.class, InvocationType.SFSB_INIT_METHOD);
                method.invoke(context.getTarget(), params);
            } catch (InvocationTargetException e) {
                if (CreateException.class.isAssignableFrom(e.getCause().getClass())) {
                    EjbExceptionTransformingInterceptorFactories.setCreateException((CreateException) e.getCause());
                }
                throw Interceptors.rethrow(e.getCause());
            } finally {
                context.putPrivateData(InvocationType.class, invocationType);
            }
        }
        return context.proceed();
    }
}



