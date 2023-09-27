/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.weld.ejb;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld.spi.ComponentInterceptorSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.bean.interceptor.InterceptorBindingsAdapter;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.injection.producer.InterceptionModelInitializer;
import org.jboss.weld.interceptor.spi.model.InterceptionModel;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;

import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;

/**
 * @author Stuart Douglas
 * @author Jozef Hartinger
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WeldInterceptorBindingsService implements Service {

    private final Consumer<InterceptorBindings> interceptorBindingsConsumer;
    private final Supplier<WeldBootstrapService> weldContainerSupplier;
    private final String beanArchiveId;
    private final String ejbName;
    private final Class<?> componentClass;
    private final ComponentInterceptorSupport interceptorSupport;

    public static final ServiceName SERVICE_NAME = ServiceName.of("WeldInterceptorBindingsService");

    public WeldInterceptorBindingsService(final Consumer<InterceptorBindings> interceptorBindingsConsumer,
                                          final Supplier<WeldBootstrapService> weldContainerSupplier,
                                          final String beanArchiveId, final String ejbName, final Class<?> componentClass,
                                          final ComponentInterceptorSupport componentInterceptorSupport) {
        this.interceptorBindingsConsumer = interceptorBindingsConsumer;
        this.weldContainerSupplier = weldContainerSupplier;
        this.beanArchiveId = beanArchiveId;
        this.ejbName = ejbName;
        this.componentClass = componentClass;
        this.interceptorSupport = componentInterceptorSupport;
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        BeanManagerImpl beanManager = weldContainerSupplier.get().getBeanManager(beanArchiveId);
        //this is not always called with the deployments TCCL set
        //which causes weld to blow up
        interceptorBindingsConsumer.accept(getInterceptorBindings(this.ejbName, beanManager));
    }

    @Override
    public void stop(final StopContext stopContext) {
        interceptorBindingsConsumer.accept(null);
    }

    private InterceptorBindings getInterceptorBindings(final String ejbName, final BeanManagerImpl manager) {
        InterceptorBindings retVal = null;
        if (ejbName != null) {
            retVal = interceptorSupport.getInterceptorBindings(ejbName, manager);
        } else {
            // This is a managed bean
            SlimAnnotatedType<?> type = (SlimAnnotatedType<?>) manager.createAnnotatedType(componentClass);
            if (!manager.getInterceptorModelRegistry().containsKey(type)) {
                EnhancedAnnotatedType<?> enhancedType = manager.getServices().get(ClassTransformer.class).getEnhancedAnnotatedType(type);
                InterceptionModelInitializer.of(manager, enhancedType, null).init();
            }
            InterceptionModel model = manager.getInterceptorModelRegistry().get(type);
            if (model != null) {
                retVal = new InterceptorBindingsAdapter(manager.getInterceptorModelRegistry().get(type));
            }
        }
        return retVal != null ? retVal : NullInterceptorBindings.INSTANCE;
    }

    private static final class NullInterceptorBindings implements InterceptorBindings {
        private static final InterceptorBindings INSTANCE = new NullInterceptorBindings();

        @Override
        public Collection<Interceptor<?>> getAllInterceptors() {
            return Collections.emptyList();
        }

        @Override
        public List<Interceptor<?>> getMethodInterceptors(final InterceptionType interceptionType, final Method method) {
            return Collections.emptyList();
        }

        @Override
        public List<Interceptor<?>> getLifecycleInterceptors(final InterceptionType interceptionType) {
            return Collections.emptyList();
        }
    }

}
