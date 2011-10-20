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

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Map;

import javax.ejb.TransactionAttributeType;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.CancellationFlag;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.ejb3.context.base.BaseSessionInvocationContext;
import org.jboss.as.ejb3.context.spi.InvocationContext;
import org.jboss.as.ejb3.context.spi.SessionContext;
import org.jboss.as.ejb3.context.spi.SessionInvocationContext;
import org.jboss.as.ejb3.tx.ApplicationExceptionDetails;
import org.jboss.as.ejb3.tx.TransactionalInvocationContext;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SessionInvocationContextInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new SessionInvocationContextInterceptor(false));
    public static final InterceptorFactory LIFECYCLE_FACTORY = new ImmediateInterceptorFactory(new SessionInvocationContextInterceptor(true));

    private final boolean lifecycleCallback;

    private SessionInvocationContextInterceptor(final boolean lifecycleCallback) {
        this.lifecycleCallback = lifecycleCallback;
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final Method invokedMethod = context.getMethod();
        final ComponentView componentView = context.getPrivateData(ComponentView.class);
        final Component component = context.getPrivateData(Component.class);
        // For a lifecycle interception, the ComponentViewInstance (and the invoked business interface) will be null.
        // On a normal method invocation, the invoked business interface will be obtained from the ComponentViewInstance
        final Class<?> invokedBusinessInterface = componentView == null ? null : componentView.getViewClass();
        Object[] parameters = context.getParameters();
        SessionInvocationContext sessionInvocationContext = new CustomSessionInvocationContext(lifecycleCallback, context, invokedBusinessInterface, invokedMethod, parameters, (EJBComponent) component);
        context.putPrivateData(InvocationContext.class, sessionInvocationContext);
        CurrentInvocationContext.push(sessionInvocationContext);
        try {
            return context.proceed();
        } finally {
            CurrentInvocationContext.pop();
            context.putPrivateData(InvocationContext.class, null);
        }
    }

    protected static class CustomSessionInvocationContext extends BaseSessionInvocationContext implements TransactionalInvocationContext {
        private final InterceptorContext context;
        private final EJBComponent component;

        protected CustomSessionInvocationContext(boolean lifecycleCallback, InterceptorContext context, Class<?> invokedBusinessInterface, Method method, Object[] parameters, final EJBComponent component) {
            super(lifecycleCallback, invokedBusinessInterface, method, parameters);

            this.context = context;
            this.component = component;
        }

        @Override
        public ApplicationExceptionDetails getApplicationException(Class<?> e) {
            return getComponent().getApplicationException(e, getMethod());
        }

        @Override
        public Principal getCallerPrincipal() {
            if (lifecycleCallback && !(component instanceof StatefulSessionComponent)) {
                throw new IllegalStateException("Cannot invoke getCallerPrincipal() in a session bean lifecycle method");
            }
            return getComponent().getCallerPrincipal();
        }

        public boolean isCallerInRole(String roleName) {
            if (lifecycleCallback && !(component instanceof StatefulSessionComponent)) {
                throw new IllegalStateException("Cannot invoke isCallerInRole() in a session bean lifecycle method");
            }
            return super.isCallerInRole(roleName);
        }

        @Override
        public SessionBeanComponent getComponent() {
            //return (SessionBeanComponent) super.getComponent();
            // this is faster
            return (SessionBeanComponent) context.getPrivateData(Component.class);
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
        public TransactionAttributeType getTransactionAttribute() {
            return getComponent().getTransactionAttributeType(getMethod());
        }

        @Override
        public int getTransactionTimeout() {
            return getComponent().getTransactionTimeout(getMethod());
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

        public InterceptorContext getContext() {
            return context;
        }
    }
}
