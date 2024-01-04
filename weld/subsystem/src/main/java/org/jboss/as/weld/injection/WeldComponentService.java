/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.injection;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionTarget;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld.spi.ComponentSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.injection.producer.InjectionTargetService;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.manager.api.WeldInjectionTarget;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Interceptor that attaches all the nessesary information for weld injection to the interceptor context
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WeldComponentService implements Service {

    private final Class<?> componentClass;
    private final Supplier<WeldBootstrapService> weldContainerSupplier;
    private final String ejbName;
    private final Set<Class<?>> interceptorClasses;
    private final Map<Class<?>, InjectionTarget> interceptorInjections = new HashMap<>();
    private final ClassLoader classLoader;
    private final String beanDeploymentArchiveId;
    private final ComponentDescription componentDescription;
    private final boolean isComponentWithView;

    /**
     * If this is true and bean is not null then weld will create the bean directly, this
     * means interceptors and decorators will be applied, which is not wanted for most component
     * types.
     */
    private final boolean delegateProduce;

    private InjectionTarget injectionTarget;
    private Bean<?> bean;
    private BeanManagerImpl beanManager;

    public WeldComponentService(final Supplier<WeldBootstrapService> weldContainerSupplier, final Class<?> componentClass, String ejbName, final Set<Class<?>> interceptorClasses, final ClassLoader classLoader, final String beanDeploymentArchiveId, final boolean delegateProduce, ComponentDescription componentDescription, final boolean isComponentWithView) {
        this.weldContainerSupplier = weldContainerSupplier;
        this.componentClass = componentClass;
        this.ejbName = ejbName;
        this.beanDeploymentArchiveId = beanDeploymentArchiveId;
        this.delegateProduce = delegateProduce;
        this.interceptorClasses = interceptorClasses;
        this.classLoader = classLoader;
        this.componentDescription = componentDescription;
        this.isComponentWithView = isComponentWithView;
    }

    WeldInjectionContext createInjectionContext() {
        return new WeldInjectionContext(beanManager.createCreationalContext(bean), bean, delegateProduce, injectionTarget, interceptorInjections);
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final ClassLoader cl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            beanManager = weldContainerSupplier.get().getBeanManager(beanDeploymentArchiveId);

            for (final Class<?> interceptor : interceptorClasses) {
                AnnotatedType<?> type = beanManager.createAnnotatedType(interceptor);
                @SuppressWarnings("rawtypes")
                InjectionTarget injectionTarget = beanManager.getInjectionTargetFactory(type).createInterceptorInjectionTarget();
                interceptorInjections.put(interceptor, beanManager.fireProcessInjectionTarget(type, injectionTarget));
            }

            if (ejbName != null) {
                EjbDescriptor<Object> descriptor = beanManager.getEjbDescriptor(ejbName);
                //may happen if the Jakarta Enterprise Beans were vetoed
                if (descriptor != null) {
                    bean = beanManager.getBean(descriptor);
                }
            }

            if (bean instanceof SessionBean<?>) {
                SessionBean<?> sessionBean = (SessionBean<?>) bean;
                this.injectionTarget = sessionBean.getProducer();
                return;
            }

            WeldInjectionTarget<?> injectionTarget = InjectionTargets.createInjectionTarget(componentClass, bean, beanManager, !isComponentWithView);

            for (ComponentSupport support : ServiceLoader.load(ComponentSupport.class,
                    WildFlySecurityManager.getClassLoaderPrivileged(WeldComponentService.class))) {
                if (support.isProcessing(componentDescription)) {
                    this.injectionTarget = support.processInjectionTarget(injectionTarget, componentDescription, beanManager);
                    break;
                }
            }
            if (this.injectionTarget == null) {
                this.injectionTarget = injectionTarget;
            }

            beanManager.getServices().get(InjectionTargetService.class).validateProducer(injectionTarget);

        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(cl);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        injectionTarget = null;
        interceptorInjections.clear();
        bean = null;
    }

    public InjectionTarget getInjectionTarget() {
        return injectionTarget;
    }

}
