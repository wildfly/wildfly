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

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.weld.services.bootstrap.WeldEjbServices;
import org.jboss.ejb3.context.CurrentInvocationContext;
import org.jboss.ejb3.context.base.BaseSessionContext;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.ejb.spi.helpers.ForwardingEjbServices;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.serialization.spi.ContextualStore;
import org.jboss.weld.serialization.spi.helpers.SerializableContextualInstance;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBContext;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interceptor for applying the JSR-299 specific interceptor bindings.
 * <p/>
 * It is a separate interceptor, as it needs to be applied after all
 * the other existing interceptors.
 *
 * @author Marius Bogoevici
 */
public class Jsr299BindingsInterceptor implements Serializable {

    private static final long serialVersionUID = -1999613731498564948L;

    private final Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> interceptorInstances = new ConcurrentHashMap<String, SerializableContextualInstance<Interceptor<Object>, Object>>();

    private volatile String ejbName;

    private volatile CreationalContext<Object> creationalContext;

    @PostConstruct
    public void doPostConstruct(InvocationContext invocationContext) throws Exception {
        final BeanManagerImpl beanManager;

        try {
            try {
                beanManager = (BeanManagerImpl) new InitialContext().lookup("java:comp/BeanManager");
            } catch (NamingException e) {
                return;
            }
            init(beanManager);
            doLifecycleInterception(invocationContext, InterceptionType.POST_CONSTRUCT);
        } finally {
            invocationContext.proceed();
        }
    }

    private void init(final BeanManagerImpl beanManager) {

        ejbName = getEjbName();
        EjbDescriptor<Object> descriptor = beanManager.getEjbDescriptor(ejbName);
        SessionBean<Object> bean = beanManager.getBean(descriptor);
        creationalContext = beanManager.createCreationalContext(bean);
        // create contextual instances for interceptors

        InterceptorBindings interceptorBindings = getInterceptorBindings(ejbName);
        if (interceptorBindings != null) {
            for (Interceptor<?> interceptor : interceptorBindings.getAllInterceptors()) {
                addInterceptorInstance((Interceptor<Object>) interceptor, beanManager);
            }

        }
    }

    private InterceptorBindings getInterceptorBindings(String ejbName) {
        //it does not matter if this is invoked twice
        final BeanManagerImpl beanManager;
        try {
            beanManager = (BeanManagerImpl) new InitialContext().lookup("java:comp/BeanManager");
        } catch (NamingException e) {
            return null;
        }

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
    private void addInterceptorInstance(Interceptor<Object> interceptor, BeanManagerImpl beanManager) {
        Object instance = beanManager.getContext(interceptor.getScope()).get(interceptor, creationalContext);
        SerializableContextualInstance<Interceptor<Object>, Object> serializableContextualInstance
                = beanManager.getServices().get(ContextualStore.class).<Interceptor<Object>, Object>getSerializableContextualInstance(interceptor, instance, creationalContext);
        interceptorInstances.put(interceptor.getBeanClass().getName(), serializableContextualInstance);
    }

    @PreDestroy
    public void doPreDestroy(InvocationContext invocationContext) throws Exception {
        try {
            doLifecycleInterception(invocationContext, InterceptionType.PRE_DESTROY);
            if (creationalContext != null) {
                creationalContext.release();
            }
        } finally {
            invocationContext.proceed();
        }
    }

    @AroundInvoke
    public Object doAroundInvoke(InvocationContext invocationContext) throws Exception {
        return doMethodInterception(invocationContext, InterceptionType.AROUND_INVOKE);
    }

    private void doLifecycleInterception(InvocationContext invocationContext, InterceptionType interceptionType)
            throws Exception {
        InterceptorBindings interceptorBindings = getInterceptorBindings(ejbName);
        if (interceptorBindings != null) {
            List<Interceptor<?>> currentInterceptors = interceptorBindings.getLifecycleInterceptors(interceptionType);
            delegateInterception(invocationContext, interceptionType, currentInterceptors);
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

    private Object delegateInterception(InvocationContext invocationContext, InterceptionType interceptionType, List<Interceptor<?>> currentInterceptors)
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

    private EJBContext getEjbContext() {
        return CurrentInvocationContext.get().getEJBContext();
    }

    private String getEjbName() {
        EJBContext ejbContext = getEjbContext();
        if (ejbContext instanceof BaseSessionContext) {
            return ((EJBComponent) ((BaseSessionContext) ejbContext).getComponent()).getComponentName();
        } else {
            throw new IllegalStateException("Unable to extract ejb name from EJBContext " + ejbContext);
        }
    }

}