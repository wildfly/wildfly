/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.weld.interceptors;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Interceptor;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld.spi.ComponentInterceptorSupport;
import org.jboss.as.weld.spi.InterceptorInstances;
import org.jboss.invocation.InterceptorContext;
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
 * @author <a href="mailto:ropalka@redhat.com>Richard Opalka</a>
 */
public class Jsr299BindingsCreateInterceptor implements org.jboss.invocation.Interceptor {

    private final Supplier<WeldBootstrapService> weldContainerSupplier;
    private final Supplier<InterceptorBindings> interceptorBindingsSupplier;
    private final String beanArchiveId;
    private final String ejbName;
    private final ComponentInterceptorSupport interceptorSupport;
    private volatile BeanManagerImpl beanManager;

    public Jsr299BindingsCreateInterceptor(final Supplier<WeldBootstrapService> weldContainerSupplier,
                                           final Supplier<InterceptorBindings> interceptorBindingsSupplier,
                                           final String beanArchiveId, final String ejbName,
                                           final ComponentInterceptorSupport interceptorSupport) {
        this.weldContainerSupplier = weldContainerSupplier;
        this.interceptorBindingsSupplier = interceptorBindingsSupplier;
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
           beanManager = this.beanManager = weldContainerSupplier.get().getBeanManager(beanArchiveId);
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
        InterceptorBindings interceptorBindings = interceptorBindingsSupplier.get();

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

}
