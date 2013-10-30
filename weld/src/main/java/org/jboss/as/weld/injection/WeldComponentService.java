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

import static org.jboss.weld.util.reflection.Reflections.cast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.servlet.AsyncListener;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.web.common.WebComponentDescription;
import org.jboss.as.webservices.injection.WSComponentDescription;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld.WeldLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.bean.ManagedBean;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.injection.producer.BasicInjectionTarget;
import org.jboss.weld.injection.producer.InjectionTargetService;
import org.jboss.weld.injection.producer.NonProducibleInjectionTarget;
import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.logging.UtilLogger;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.util.Beans;
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

    /**
     * If this is true and bean is not null then weld will create the bean directly, this
     * means interceptors and decorators will be applied, which is not wanted for most component
     * types.
     */
    private final boolean delegateProduce;

    private InjectionTarget injectionTarget;
    private Bean<?> bean;
    private BeanManagerImpl beanManager;

    public WeldComponentService(Class<?> componentClass, String ejbName, final Set<Class<?>> interceptorClasses, final ClassLoader classLoader, final String beanDeploymentArchiveId, final boolean delegateProduce, ComponentDescription componentDescription) {
        this.componentClass = componentClass;
        this.ejbName = ejbName;
        this.beanDeploymentArchiveId = beanDeploymentArchiveId;
        this.delegateProduce = delegateProduce;
        this.weldContainer = new InjectedValue<WeldBootstrapService>();
        this.interceptorClasses = interceptorClasses;
        this.classLoader = classLoader;
        this.componentDescription = componentDescription;
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
                interceptorInjections.put(interceptor, createInjectionTarget(interceptor, null, beanManager));
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
                this.injectionTarget = sessionBean.getInjectionTarget();
                return;
            }

            if (componentDescription instanceof WSComponentDescription) {
                ManagedBean<?> bean = findManagedBeanForWSComponent(componentClass);
                if (bean != null) {
                    injectionTarget = bean.getInjectionTarget();
                    return;
                }
            }

            BasicInjectionTarget injectionTarget = createInjectionTarget(componentClass, bean, beanManager);
            if (componentDescription instanceof MessageDrivenComponentDescription || componentDescription instanceof WebComponentDescription) {
                // fire ProcessInjectionTarget for non-contextual components
                this.injectionTarget = beanManager.fireProcessInjectionTarget(injectionTarget.getAnnotated(), injectionTarget);
            } else {
                this.injectionTarget = injectionTarget;
            }
            beanManager.getServices().get(InjectionTargetService.class).validateProducer(injectionTarget);

        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(cl);
        }
    }

    private <T> BasicInjectionTarget<T> createInjectionTarget(Class<?> componentClass, Bean<T> bean, BeanManagerImpl beanManager) {
        EnhancedAnnotatedType<T> type = beanManager.getServices().get(ClassTransformer.class).getEnhancedAnnotatedType((Class<T>) componentClass, beanManager.getId());
        if (Beans.getBeanConstructor(type) == null) {
            if (AsyncListener.class.isAssignableFrom(componentClass)) {
                /*
                 * AsyncListeners may be CDI-incompatible as long as the application never calls
                 * javax.servletAsyncContext#createListener(Class) and only instantiates the listener
                 * itself.
                 */
                return new NonProducibleInjectionTarget<>(type, bean, beanManager);
            } else {
                throw UtilLogger.LOG.unableToFindConstructor(type);
            }
        }
        return new NonContextualComponentInjectionTarget<>(type, bean, beanManager);
    }

    private <T> ManagedBean<T> findManagedBeanForWSComponent(Class<T> definingClass) {
        Set<Bean<?>> beans = new HashSet<Bean<?>>(beanManager.getBeans(definingClass, AnyLiteral.INSTANCE));
        for (Iterator<Bean<?>> i = beans.iterator(); i.hasNext();) {
            Bean<?> bean = i.next();
            if (bean instanceof ManagedBean<?> && bean.getBeanClass().equals(definingClass)) {
                continue;
            }
            i.remove();
        }
        if (beans.isEmpty()) {
            WeldLogger.DEPLOYMENT_LOGGER.debugf("Could not find bean for %s, interception and decoration will be unavailable", componentClass);
            return null;
        }
        if (beans.size() > 1) {
            WeldLogger.DEPLOYMENT_LOGGER.debugf("Multiple beans for %s : %s ", componentClass, beans);
        }
        return cast(beans.iterator().next());
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
}
