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

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.weld.context.ejb.EjbRequestContext;
import org.jboss.weld.ejb.AbstractEJBRequestScopeActivationInterceptor;
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
 * @author Jozef Hartinger
 */
public class EjbRequestScopeActivationInterceptor extends AbstractEJBRequestScopeActivationInterceptor implements Serializable, org.jboss.invocation.Interceptor {

    private static final long serialVersionUID = -503029523442133584L;

    private volatile EjbRequestContext requestContext;
    private volatile BeanManagerImpl beanManager;
    private final ServiceName weldContainerServiceName;

    public EjbRequestScopeActivationInterceptor(final ServiceName weldContainerServiceName) {
        this.weldContainerServiceName = weldContainerServiceName;
    }

    @Override
    protected BeanManagerImpl getBeanManager() {
        // get the reference to the bean manager on the first invocation
        if (beanManager == null) {
            final WeldBootstrapService weldContainer = (WeldBootstrapService) currentServiceContainer().getRequiredService(weldContainerServiceName).getValue();
            beanManager = (BeanManagerImpl) weldContainer.getBeanManager();
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

        public Factory(final ServiceName weldContainerServiceName) {
            this.interceptor = new EjbRequestScopeActivationInterceptor(weldContainerServiceName);
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
