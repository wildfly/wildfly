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

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.weld.WeldContainer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.manager.BeanManagerImpl;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Managed reference factory that can be used to create and inject components.
 *
 * @author Stuart Douglas
 */
public class WeldManagedReferenceFactory implements ManagedReferenceFactory, Service<WeldManagedReferenceFactory> {

    private final Class<?> componentClass;
    private final InjectedValue<WeldContainer> weldContainer;
    private final String ejbName;
    private final Set<Class<?>> interceptorClasses;
    private final Map<Class<?>, WeldEEInjection> interceptorInjections = new HashMap<Class<?>, WeldEEInjection>();
    private final ClassLoader classLoader;
    private final String beanDeploymentArchiveId;

    private WeldEEInjection injectionTarget;
    private Bean<?> bean;
    private BeanManagerImpl beanManager;

    public WeldManagedReferenceFactory(Class<?> componentClass, String ejbName, final Set<Class<?>> interceptorClasses, final ClassLoader classLoader, final String beanDeploymentArchiveId) {
        this.componentClass = componentClass;
        this.ejbName = ejbName;
        this.beanDeploymentArchiveId = beanDeploymentArchiveId;
        this.weldContainer = new InjectedValue<WeldContainer>();
        this.interceptorClasses = interceptorClasses;
        this.classLoader = classLoader;
    }

    @Override
    public ManagedReference getReference() {
        final CreationalContext<?> ctx;
        if (bean == null) {
            ctx = beanManager.createCreationalContext(null);
        } else {
            ctx = beanManager.createCreationalContext(bean);
        }
        final Object instance = injectionTarget.produce(ctx);
        return new WeldManagedReference(ctx, instance, injectionTarget, interceptorInjections);
    }

    public ManagedReference injectExistingReference(final ManagedReference existing) {
        final CreationalContext<?> ctx;
        if (bean == null) {
            ctx = beanManager.createCreationalContext(null);
        } else {
            ctx = beanManager.createCreationalContext(bean);
        }
        final Object instance = existing.getInstance();

        injectionTarget.inject(instance, ctx);

        return new ManagedReference() {
            @Override
            public void release() {
                try {
                    existing.release();
                } finally {
                    ctx.release();
                }
            }

            @Override
            public Object getInstance() {
                return instance;
            }
        };
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
    public synchronized WeldManagedReferenceFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<WeldContainer> getWeldContainer() {
        return weldContainer;
    }
}
