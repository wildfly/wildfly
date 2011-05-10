/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.CancellationFlag;
import org.jboss.ejb3.context.CurrentInvocationContext;
import org.jboss.ejb3.context.base.BaseSessionInvocationContext;
import org.jboss.ejb3.context.spi.InvocationContext;
import org.jboss.ejb3.context.spi.SessionContext;
import org.jboss.ejb3.context.spi.SessionInvocationContext;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SessionInvocationContextInterceptor implements Interceptor {

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        // TODO: fill in the proper invoked business interface
        Class<?> invokedBusinessInterface = null;
        Method method = context.getMethod();
        Object[] parameters = context.getParameters();
        SessionInvocationContext sessionInvocationContext = new CustomSessionInvocationContext(context, invokedBusinessInterface, method, parameters);
        context.putPrivateData(InvocationContext.class, sessionInvocationContext);
        CurrentInvocationContext.push(sessionInvocationContext);
        try {
            return context.proceed();
        } finally {
            CurrentInvocationContext.pop();
            context.putPrivateData(InvocationContext.class, null);
        }
    }

    protected static class CustomSessionInvocationContext extends BaseSessionInvocationContext {
        private InterceptorContext context;

        protected CustomSessionInvocationContext(InterceptorContext context, Class<?> invokedBusinessInterface, Method method, Object[] parameters) {
            super(invokedBusinessInterface, method, parameters);

            this.context = context;
        }

        @Override
        public Map<String, Object> getContextData() {
            return context.getContextData();
        }

        @Override
        public SessionContext getEJBContext() {
            return ((SessionBeanComponentInstance) context.getPrivateData(ComponentInstance.class)).getSessionContext();
        }

        @Override
        public Object proceed() throws Exception {
            return context.proceed();
        }

        @Override
        public void setParameters(Object[] params) throws IllegalArgumentException, IllegalStateException {
            context.setParameters(params);
        }

        public boolean wasCancelCalled() throws IllegalStateException {
            final CancellationFlag flag = context.getPrivateData(CancellationFlag.class);
            if (flag != null) {
                return flag.get();
            }
            return super.wasCancelCalled();
        }
    }
}
