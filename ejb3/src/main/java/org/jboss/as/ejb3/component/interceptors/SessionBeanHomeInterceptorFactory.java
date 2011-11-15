/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
