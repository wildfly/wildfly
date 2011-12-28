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
package org.jboss.as.ejb3.component.session;

import javax.ejb.SessionBean;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Interceptor that invokes the {@link SessionBean#setSessionContext(javax.ejb.SessionContext)} on session beans
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
