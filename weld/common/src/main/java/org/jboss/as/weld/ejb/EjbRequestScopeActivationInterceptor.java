/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Inc., and individual contributors as indicated
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

package org.jboss.as.weld.ejb;

import java.io.Serializable;
import java.security.AccessController;

import javax.enterprise.inject.spi.BeanManager;
import javax.interceptor.InvocationContext;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.context.ejb.EjbRequestContext;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.module.ejb.AbstractEJBRequestScopeActivationInterceptor;

/**
 * Interceptor for activating the CDI request scope on some EJB invocations.
 * <p>
 * Remote EJB invocations must also have the request scope active, but it may already be active for in-VM requests.
 * <p>
 * This interceptor is largely stateless, and can be re-used
 * <p>
 * Note that {@link EjbRequestContext} is actually bound to {@link InvocationContext} and so it's ok to use this interceptor for other components than ejbs.
 *
 * @author Stuart Douglas
 * @author Jozef Hartinger
 */
public class EjbRequestScopeActivationInterceptor extends AbstractEJBRequestScopeActivationInterceptor implements Serializable, org.jboss.invocation.Interceptor {

    private static final long serialVersionUID = -503029523442133584L;

    private volatile EjbRequestContext requestContext;
    private volatile BeanManagerImpl beanManager;
    private final ServiceName beanManagerServiceName;

    public EjbRequestScopeActivationInterceptor(final ServiceName beanManagerServiceName) {
        this.beanManagerServiceName = beanManagerServiceName;
    }

    @Override
    protected BeanManagerImpl getBeanManager() {
        // get the reference to the bean manager on the first invocation
        if (beanManager == null) {
            beanManager = BeanManagerProxy.unwrap((BeanManager)currentServiceContainer().getRequiredService(beanManagerServiceName).getValue());
        }
        return beanManager;
    }

    @Override
    protected EjbRequestContext getEjbRequestContext() {
        //create the context lazily, on the first invocation
        //we can't do this on interceptor creation, as the timed object invoker may create the interceptor
        //before we have been injected
        if (requestContext == null) {
            requestContext = super.getEjbRequestContext();
        }
        return requestContext;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        return aroundInvoke(context.getInvocationContext());
    }


    public static class Factory implements InterceptorFactory {

        private final Interceptor interceptor;

        public Factory(final ServiceName beanManagerServiceName) {
            this.interceptor = new EjbRequestScopeActivationInterceptor(beanManagerServiceName);
        }

        @Override
        public org.jboss.invocation.Interceptor create(final InterceptorFactoryContext context) {
            return interceptor;
        }
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
