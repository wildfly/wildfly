/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.interceptors;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Interceptor that can return a home interface for a Jakarta Enterprise Beans bean
 *
 * @author Stuart Douglas
 */
public class GetHomeInterceptorFactory implements InterceptorFactory {

    private final InjectedValue<ComponentView> viewToCreate = new InjectedValue<ComponentView>();

    private final Interceptor interceptor;

    public GetHomeInterceptorFactory() {
        interceptor = new Interceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {
                return viewToCreate.getValue().createInstance().getInstance();
            }
        };
    }

    @Override
    public Interceptor create(final InterceptorFactoryContext context) {
        return interceptor;
    }


    public InjectedValue<ComponentView> getViewToCreate() {
        return viewToCreate;
    }
}
