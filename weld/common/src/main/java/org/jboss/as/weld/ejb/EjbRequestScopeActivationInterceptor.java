/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.weld.ejb;

import java.io.Serializable;
import java.security.AccessController;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.interceptor.InvocationContext;

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
 * Interceptor for activating the Jakarta Contexts and Dependency Injection request scope on some Jakarta Enterprise Beans invocations.
 * <p>
 * Remote Jakarta Enterprise Beans invocations must also have the request scope active, but it may already be active for in-VM requests.
 * <p>
 * This interceptor is largely stateless, and can be re-used
 * <p>
 * Note that {@link EjbRequestContext} is actually bound to {@link InvocationContext} and so it's ok to use this interceptor for other components than Jakarta Enterprise Beans.
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
