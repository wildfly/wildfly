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
package org.jboss.as.ejb3.component.stateful;

import org.jboss.as.ejb3.component.interceptors.AbstractEJBInterceptor;
import org.jboss.as.ejb3.component.session.SessionBeanHomeInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

import java.lang.reflect.Method;

/**
 * Interceptor factory for SFSB's that invokes the ejbCreate method. This interceptor
 * is only used when a component is created via a home interface method.
 *
 * @author Stuart Douglas
 */
public class StatefulInitMethodInterceptorFactory implements InterceptorFactory {

    public static final InterceptorFactory INSTANCE = new StatefulInitMethodInterceptorFactory();

    private StatefulInitMethodInterceptorFactory() {

    }


    @Override
    public Interceptor create(final InterceptorFactoryContext context) {
        final Method method = SessionBeanHomeInterceptorFactory.INIT_METHOD.get();
        final Object[] params = SessionBeanHomeInterceptorFactory.INIT_PARAMETERS.get();
        return new AbstractEJBInterceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {
                if (method != null) {
                    method.invoke(context.getTarget(), params);
                }
                return context.proceed();
            }
        };
    }
}
