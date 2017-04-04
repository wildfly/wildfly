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

package org.jboss.as.weld.interceptors;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Interceptor;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld.spi.ComponentInterceptorSupport;
import org.jboss.as.weld.spi.InterceptorInstances;
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
    private final ComponentInterceptorSupport interceptorSupport;
    private volatile BeanManagerImpl beanManager;

    public Jsr299BindingsCreateInterceptor(String beanArchiveId, String ejbName, ComponentInterceptorSupport interceptorSupport) {
        this.beanArchiveId = beanArchiveId;
        this.ejbName = ejbName;
        this.interceptorSupport = interceptorSupport;
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
        InterceptorInstances existing = interceptorSupport.getInterceptorInstances(componentInstance);

        if (existing == null) {
            CreationalContext<Object> creationalContext = beanManager.createCreationalContext(bean);
            HashMap<String, SerializableContextualInstance<Interceptor<Object>, Object>> interceptorInstances = new HashMap<String, SerializableContextualInstance<Interceptor<Object>, Object>>();

            if (interceptorBindings != null) {
                for (Interceptor<?> interceptor : interceptorBindings.getAllInterceptors()) {
                    addInterceptorInstance((Interceptor<Object>) interceptor, beanManager, interceptorInstances, creationalContext);
                }
            }
            interceptorSupport.setInterceptorInstances(componentInstance, new WeldInterceptorInstances(creationalContext, interceptorInstances));
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
