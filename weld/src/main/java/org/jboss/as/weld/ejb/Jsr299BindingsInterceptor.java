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

import org.jboss.as.weld.WeldContainer;
import org.jboss.as.weld.services.bootstrap.WeldEjbServices;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.ejb.spi.helpers.ForwardingEjbServices;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.serialization.spi.ContextualStore;
import org.jboss.weld.serialization.spi.helpers.SerializableContextualInstance;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interceptor for applying the JSR-299 specific interceptor bindings.
 * <p/>
 * It is a separate interceptor, as it needs to be applied after all
 * the other existing interceptors.
 *
 * @author Marius Bogoevici
 * @author Stuart Douglas
 */
public class Jsr299BindingsInterceptor implements Serializable, org.jboss.invocation.Interceptor {

    private final Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> interceptorInstances;
    private final CreationalContext<Object> creationalContext;
    private final String ejbName;
    private final BeanManager beanManager;
    private final InterceptionType interceptionType;


    protected Jsr299BindingsInterceptor(final BeanManagerImpl beanManager, final String ejbName, final InterceptorFactoryContext context, final InterceptionType interceptionType) {

        this.beanManager = beanManager;
        this.ejbName = ejbName;
        this.interceptionType = interceptionType;
        EjbDescriptor<Object> descriptor = beanManager.getEjbDescriptor(this.ejbName);
        SessionBean<Object> bean = beanManager.getBean(descriptor);
        InterceptorInstances instances = (InterceptorInstances) context.getContextData().get(InterceptorInstances.class);

        if (instances == null) {
            creationalContext = beanManager.createCreationalContext(bean);
            interceptorInstances = new HashMap<String, SerializableContextualInstance<Interceptor<Object>, Object>>();
            InterceptorBindings interceptorBindings = getInterceptorBindings(this.ejbName);
            if (interceptorBindings != null) {
                for (Interceptor<?> interceptor : interceptorBindings.getAllInterceptors()) {
                    addInterceptorInstance((Interceptor<Object>) interceptor, beanManager, interceptorInstances);
                }
            }
            instances = new InterceptorInstances(creationalContext, interceptorInstances);
            context.getContextData().put(InterceptorInstances.class, instances);
        } else {
            creationalContext = instances.creationalContext;
            interceptorInstances = instances.interceptorInstances;
        }

    }


    protected Object delegateInterception(InvocationContext invocationContext, InterceptionType interceptionType, List<Interceptor<?>> currentInterceptors)
            throws Exception {
        List<Object> currentInterceptorInstances = new ArrayList<Object>();
        for (Interceptor<?> interceptor : currentInterceptors) {
            currentInterceptorInstances.add(interceptorInstances.get(interceptor.getBeanClass().getName()).getInstance());
        }
        if (currentInterceptorInstances.size() > 0) {
            return new DelegatingInterceptorInvocationContext(invocationContext, currentInterceptors, currentInterceptorInstances, interceptionType).proceed();
        } else {
            return invocationContext.proceed();
        }

    }


    private Object doMethodInterception(InvocationContext invocationContext, InterceptionType interceptionType)
            throws Exception {
        InterceptorBindings interceptorBindings = getInterceptorBindings(ejbName);
        if (interceptorBindings != null) {
            List<Interceptor<?>> currentInterceptors = interceptorBindings.getMethodInterceptors(interceptionType, invocationContext.getMethod());
            return delegateInterception(invocationContext, interceptionType, currentInterceptors);
        } else {
            return invocationContext.proceed();
        }
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        switch (interceptionType) {
            case AROUND_INVOKE:
                return doMethodInterception(context.getInvocationContext(), InterceptionType.AROUND_INVOKE);
            case PRE_DESTROY:
                try {
                    return doLifecycleInterception(context);
                } finally {
                    creationalContext.release();
                }
            case POST_CONSTRUCT:
                return doLifecycleInterception(context);
            default:
                //should never happen
                return context.proceed();
        }
    }

    private Object doLifecycleInterception(final InterceptorContext context) throws Exception {
        try {
            final InterceptorBindings interceptorBindings = getInterceptorBindings(ejbName);
            if (interceptorBindings != null) {
                List<Interceptor<?>> currentInterceptors = interceptorBindings.getLifecycleInterceptors(interceptionType);
                delegateInterception(context.getInvocationContext(), interceptionType, currentInterceptors);
            }
        } finally {
            return context.proceed();
        }
    }

    protected InterceptorBindings getInterceptorBindings(String ejbName) {
        BeanManagerImpl beanManager = (BeanManagerImpl) this.beanManager;
        EjbServices ejbServices = beanManager.getServices().get(EjbServices.class);
        if (ejbServices instanceof ForwardingEjbServices) {
            ejbServices = ((ForwardingEjbServices) ejbServices).delegate();
        }
        InterceptorBindings interceptorBindings = null;
        if (ejbServices instanceof WeldEjbServices) {
            interceptorBindings = ((WeldEjbServices) ejbServices).getBindings(ejbName);
        }
        return interceptorBindings;
    }

    @SuppressWarnings("unchecked")
    private void addInterceptorInstance(Interceptor<Object> interceptor, BeanManagerImpl beanManager, Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> instances) {
        Object instance = beanManager.getContext(interceptor.getScope()).get(interceptor, creationalContext);
        SerializableContextualInstance<Interceptor<Object>, Object> serializableContextualInstance
                = beanManager.getServices().get(ContextualStore.class).<Interceptor<Object>, Object>getSerializableContextualInstance(interceptor, instance, creationalContext);
        instances.put(interceptor.getBeanClass().getName(), serializableContextualInstance);
    }


    public static class Factory implements InterceptorFactory {

        private final InjectedValue<WeldContainer> weldContainer = new InjectedValue<WeldContainer>();
        private final String beanArchiveId;
        private final String ejbName;
        private final InterceptionType interceptionType;

        public Factory(final String beanArchiveId, final String ejbName, final InterceptionType interceptionType) {
            this.beanArchiveId = beanArchiveId;
            this.ejbName = ejbName;
            this.interceptionType = interceptionType;
        }

        @Override
        public org.jboss.invocation.Interceptor create(final InterceptorFactoryContext context) {
            return new Jsr299BindingsInterceptor((BeanManagerImpl) weldContainer.getValue().getBeanManager(beanArchiveId), ejbName, context, interceptionType);
        }

        public InjectedValue<WeldContainer> getWeldContainer() {
            return weldContainer;
        }
    }

    private static class InterceptorInstances {
        protected final CreationalContext<Object> creationalContext;
        protected final Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> interceptorInstances;

        public InterceptorInstances(final CreationalContext<Object> creationalContext, final Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> interceptorInstances) {
            this.creationalContext = creationalContext;
            this.interceptorInstances = interceptorInstances;
        }
    }
}