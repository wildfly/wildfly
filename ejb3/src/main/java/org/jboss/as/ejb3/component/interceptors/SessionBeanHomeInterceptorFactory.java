/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.interceptors;

import java.lang.reflect.Method;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Interceptor that handles home views for session beans
 *
 * @author Stuart Douglas
 */
public class SessionBeanHomeInterceptorFactory implements InterceptorFactory {

    private final InjectedValue<ComponentView> viewToCreate = new InjectedValue<ComponentView>();

    //TODO: there has to be a better way to pass this into the create interceptor chain
    public static final ThreadLocal<Method> INIT_METHOD = new ThreadLocal<Method>();

    public static final ThreadLocal<Object[]> INIT_PARAMETERS = new ThreadLocal<Object[]>();

    /**
     * The init method to invoke on the SFSB
     */
    private final Method method;

    public SessionBeanHomeInterceptorFactory(final Method method) {
        this.method = method;
    }

    @Override
    public Interceptor create(final InterceptorFactoryContext context) {
        return new Interceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {
                final ComponentView view = viewToCreate.getValue();
                try {
                    INIT_METHOD.set(method);
                    INIT_PARAMETERS.set(context.getParameters());
                    final ManagedReference instance = view.createInstance();
                    return instance.getInstance();
                } finally {
                    INIT_METHOD.remove();
                    INIT_PARAMETERS.remove();
                }
            }
        };
    }

    public InjectedValue<ComponentView> getViewToCreate() {
        return viewToCreate;
    }
}
