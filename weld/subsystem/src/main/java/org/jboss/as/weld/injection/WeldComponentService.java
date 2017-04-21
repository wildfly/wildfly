/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.weld.injection;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld.spi.ComponentSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
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
 */
public class WeldComponentService implements Service<WeldComponentService> {

    private final Class<?> componentClass;
    private final InjectedValue<WeldBootstrapService> weldContainer;
    private final String ejbName;
    private final Set<Class<?>> interceptorClasses;
    private final Map<Class<?>, InjectionTarget> interceptorInjections = new HashMap<Class<?>, InjectionTarget>();
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

    public WeldComponentService(Class<?> componentClass, String ejbName, final Set<Class<?>> interceptorClasses, final ClassLoader classLoader, final String beanDeploymentArchiveId, final boolean delegateProduce, ComponentDescription componentDescription, boolean isComponentWithView) {
        this.componentClass = componentClass;
        this.ejbName = ejbName;
        this.beanDeploymentArchiveId = beanDeploymentArchiveId;
        this.delegateProduce = delegateProduce;
        this.weldContainer = new InjectedValue<WeldBootstrapService>();
        this.interceptorClasses = interceptorClasses;
        this.classLoader = classLoader;
        this.componentDescription = componentDescription;
        this.isComponentWithView = isComponentWithView;
    }

    public WeldInjectionContext createInjectionContext() {
        return new WeldInjectionContext(beanManager.createCreationalContext(bean), bean, delegateProduce, injectionTarget, interceptorInjections);
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final ClassLoader cl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            beanManager = weldContainer.getValue().getBeanManager(beanDeploymentArchiveId);

            for (final Class<?> interceptor : interceptorClasses) {
                AnnotatedType<?> type = beanManager.createAnnotatedType(interceptor);
                @SuppressWarnings("rawtypes")
                InjectionTarget injectionTarget = beanManager.getInjectionTargetFactory(type).createInterceptorInjectionTarget();
                interceptorInjections.put(interceptor, beanManager.fireProcessInjectionTarget(type, injectionTarget));
            }

            if (ejbName != null) {
                EjbDescriptor<Object> descriptor = beanManager.getEjbDescriptor(ejbName);
                //may happen if the EJB was vetoed
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

    @Override
    public synchronized WeldComponentService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<WeldBootstrapService> getWeldContainer() {
        return weldContainer;
    }

    public InjectionTarget getInjectionTarget() {
        return injectionTarget;
    }
}
