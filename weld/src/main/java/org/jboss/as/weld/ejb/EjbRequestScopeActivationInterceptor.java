/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.weld.ejb;

import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.weld.WeldContainer;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.weld.context.ejb.EjbLiteral;
import org.jboss.weld.context.ejb.EjbRequestContext;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * Interceptor for activating the CDI request scope on some EJB invocations.
 * <p/>
 * For efficiency reasons we do not check to see if the scope is already active. For this reason this interceptor
 * can only be used for request paths where we know that the scope is not active, such as MDB's and the timer service.
 * <p/>
 * Remote EJB invocations must also have the request scope active, but it may already be active for in-VM requests.
 * <p/>
 * This interceptor is largely stateless, and can be re-used
 *
 * @author Stuart Douglas
 */
public class EjbRequestScopeActivationInterceptor implements Serializable, org.jboss.invocation.Interceptor {

    private volatile EjbRequestContext requestContext;
    private volatile BeanManagerImpl beanManager;
    private final ServiceName weldContainerServiceName;

    public EjbRequestScopeActivationInterceptor(final ServiceName weldContainerServiceName) {
        this.weldContainerServiceName = weldContainerServiceName;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        //get the reference to the bean manager on the first invocation
        if(beanManager == null) {
            final WeldContainer weldContainer = (WeldContainer) currentServiceContainer().getRequiredService(weldContainerServiceName).getValue();
            beanManager = (BeanManagerImpl) weldContainer.getBeanManager();
        }

        try {
            //if this call succeeds then the context is active
            beanManager.getContext(RequestScoped.class);
            return context.proceed();
        } catch (ContextNotActiveException exception) {
        }


        //create the context lazily, on the first invocation
        //we can't do this on interceptor creation, as the timed object invoker may create the interceptor
        //before we have been injected
        if (requestContext == null) {
            //it does not matter if this happens twice
            //we just look up the service rather than using a dependency
            //the component itself has a dependency on this service, which means that it has to be up

            final Bean<?> bean = beanManager.resolve(beanManager.getBeans(EjbRequestContext.class, EjbLiteral.INSTANCE));
            final CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
            requestContext = (EjbRequestContext) beanManager.getReference(bean, EjbRequestContext.class, ctx);
        }
        try {
            requestContext.associate(context.getInvocationContext());
            requestContext.activate();
            return context.proceed();
        } finally {
            requestContext.invalidate();
            requestContext.deactivate();
            requestContext.dissociate(context.getInvocationContext());
        }
    }


    public static class Factory implements InterceptorFactory {

        private final Interceptor interceptor;

        public Factory(final ServiceName weldContainerServiceName) {
            this.interceptor = new EjbRequestScopeActivationInterceptor(weldContainerServiceName);
        }

        @Override
        public org.jboss.invocation.Interceptor create(final InterceptorFactoryContext context) {
            return interceptor;
        }
    }

    private static ServiceContainer currentServiceContainer() {
        return AccessController.doPrivileged(new PrivilegedAction<ServiceContainer>() {
            @Override
            public ServiceContainer run() {
                return CurrentServiceContainer.getServiceContainer();
            }
        });
    }
}