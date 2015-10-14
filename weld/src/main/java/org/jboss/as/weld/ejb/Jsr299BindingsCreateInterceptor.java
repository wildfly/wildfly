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

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Interceptor;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.stateful.SerializedCdiInterceptorsKey;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.serialization.spi.ContextualStore;
import org.jboss.weld.serialization.spi.helpers.SerializableContextualInstance;

/**
 * Interceptor that creates the CDI interceptors and attaches them to the component
 *
 * @author Marius Bogoevici
 * @author Stuart Douglas
 */
public class Jsr299BindingsCreateInterceptor implements org.jboss.invocation.Interceptor {


    private final InjectedValue<WeldBootstrapService> weldContainer = new InjectedValue<WeldBootstrapService>();
    private final InjectedValue<InterceptorBindings> interceptorBindings = new InjectedValue<InterceptorBindings>();
    private final String beanArchiveId;
    private final String ejbName;
    private volatile BeanManagerImpl beanManager;

    public Jsr299BindingsCreateInterceptor(String beanArchiveId, String ejbName) {
        this.beanArchiveId = beanArchiveId;
        this.ejbName = ejbName;
    }


    private void addInterceptorInstance(Interceptor<Object> interceptor, BeanManagerImpl beanManager, Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> instances, final CreationalContext<Object> creationalContext) {
        Object instance = beanManager.getReference(interceptor, interceptor.getBeanClass(), creationalContext, true);
        SerializableContextualInstance<Interceptor<Object>, Object> serializableContextualInstance
                = beanManager.getServices().get(ContextualStore.class).<Interceptor<Object>, Object>getSerializableContextualInstance(interceptor, instance, creationalContext);
        instances.put(interceptor.getBeanClass().getName(), serializableContextualInstance);
    }

    @Override
    public Object processInvocation(InterceptorContext interceptorContext) throws Exception {

        BeanManagerImpl beanManager = this.beanManager;
        if(beanManager == null) {
            //cache the BM lookup, as it is quite slow
           beanManager = this.beanManager = this.weldContainer.getValue().getBeanManager(beanArchiveId);
        }
        //this is not always called with the deployments TCCL set
        //which causes weld to blow up
        SessionBean<Object> bean = null;
        if (ejbName != null) {
            EjbDescriptor<Object> descriptor = beanManager.getEjbDescriptor(this.ejbName);
            if (descriptor != null) {
                bean = beanManager.getBean(descriptor);
            }
        }
        InterceptorBindings interceptorBindings = this.interceptorBindings.getValue();

        final ComponentInstance componentInstance = interceptorContext.getPrivateData(ComponentInstance.class);
        WeldInterceptorInstances existing = (WeldInterceptorInstances) componentInstance.getInstanceData(SerializedCdiInterceptorsKey.class);

        if (existing == null) {
            CreationalContext<Object> creationalContext = beanManager.createCreationalContext(bean);
            HashMap<String, SerializableContextualInstance<Interceptor<Object>, Object>> interceptorInstances = new HashMap<String, SerializableContextualInstance<Interceptor<Object>, Object>>();

            if (interceptorBindings != null) {
                for (Interceptor<?> interceptor : interceptorBindings.getAllInterceptors()) {
                    addInterceptorInstance((Interceptor<Object>) interceptor, beanManager, interceptorInstances, creationalContext);
                }
            }
            WeldInterceptorInstances instances = new WeldInterceptorInstances(creationalContext, interceptorInstances);
            componentInstance.setInstanceData(SerializedCdiInterceptorsKey.class, instances);
        }
        return interceptorContext.proceed();
    }

    public InjectedValue<WeldBootstrapService> getWeldContainer() {
        return weldContainer;
    }

    public InjectedValue<InterceptorBindings> getInterceptorBindings() {
        return interceptorBindings;
    }
}
