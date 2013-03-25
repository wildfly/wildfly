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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;

import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld.WeldLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.bean.ManagedBean;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.manager.BeanManagerImpl;

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
    private final Map<Class<?>, WeldEEInjection> interceptorInjections = new HashMap<Class<?>, WeldEEInjection>();
    private final ClassLoader classLoader;
    private final String beanDeploymentArchiveId;

    /**
     * If this is true and bean is not null then weld will create the bean directly, this
     * means interceptors and decorators will be applied, which is not wanted for most component
     * types.
     */
    private final boolean delegateProduce;

    private WeldEEInjection injectionTarget;
    private Bean<?> bean;
    private BeanManagerImpl beanManager;

    public WeldComponentService(Class<?> componentClass, String ejbName, final Set<Class<?>> interceptorClasses, final ClassLoader classLoader, final String beanDeploymentArchiveId, final boolean delegateProduce) {
        this.componentClass = componentClass;
        this.ejbName = ejbName;
        this.beanDeploymentArchiveId = beanDeploymentArchiveId;
        this.delegateProduce = delegateProduce;
        this.weldContainer = new InjectedValue<WeldBootstrapService>();
        this.interceptorClasses = interceptorClasses;
        this.classLoader = classLoader;
    }

    public WeldInjectionContext createInjectionContext() {
        return new WeldInjectionContext(beanManager.createCreationalContext(bean), bean, delegateProduce, injectionTarget, interceptorInjections);
    }


    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final ClassLoader cl = SecurityActions.getContextClassLoader();
        try {
            SecurityActions.setContextClassLoader(classLoader);
            beanManager = (BeanManagerImpl) weldContainer.getValue().getBeanManager(beanDeploymentArchiveId);

            for (final Class<?> interceptor : interceptorClasses) {
                interceptorInjections.put(interceptor, WeldEEInjection.createWeldEEInjection(interceptor, null, beanManager));
            }

            if (ejbName != null) {
                EjbDescriptor<Object> descriptor = beanManager.getEjbDescriptor(ejbName);
                //may happen if the EJB was vetoed
                if (descriptor != null) {
                    bean = beanManager.getBean(descriptor);
                }
            } else if (delegateProduce) {
                final Set<Annotation> qualifiers = new HashSet<Annotation>();
                for(Annotation annotation : componentClass.getAnnotations()) {
                    if(beanManager.isQualifier(annotation.annotationType())) {
                        qualifiers.add(annotation);
                    }
                }
                Set<Bean<?>> beans = beanManager.getBeans(componentClass, qualifiers);
                boolean found = false;
                for(Bean<?> bean : beans) {
                    if(bean instanceof ManagedBean) {
                        found = true;
                        this.bean = bean;
                        break;
                    }
                }
                if(!found){
                    WeldLogger.DEPLOYMENT_LOGGER.debugf("Could not find bean for %s, interception and decoration will be unavailable", componentClass);
                }
            }
            injectionTarget = WeldEEInjection.createWeldEEInjection(componentClass, bean, beanManager);

        } finally {
            SecurityActions.setContextClassLoader(cl);
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
}
