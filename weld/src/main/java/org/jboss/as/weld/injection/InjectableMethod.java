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

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.as.weld.WeldMessages;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * tracks initalizer methods
 */
final class InjectableMethod {
    private final Method method;
    private final List<Bean<?>> beans;
    private final List<InjectionPoint> injectionPoints;

    public InjectableMethod(final Method method, final List<Bean<?>> beans, final List<InjectionPoint> injectionPoints) {
        this.beans = beans;
        this.method = method;
        this.injectionPoints = injectionPoints;
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                method.setAccessible(true);
                return null;
            }
        });
    }


    /**
     * Invokes an Inject annotated method
     * @param instance The instance to invoke on
     * @param beanManager The current BeanManager
     * @param ctx The creational context
     */
    public void inject(Object instance, BeanManagerImpl beanManager, CreationalContext<?> ctx) {
        try {
            final Object[] params = new Object[beans.size()];
            int i = 0;
            for(Bean<?> bean : beans) {
                final Object value = beanManager.getReference(injectionPoints.get(i),bean, ctx);
                params[i++] = value;
            }
            method.invoke(instance,params);
        } catch (Exception e) {
            throw WeldMessages.MESSAGES.couldNotInjectMethod(method, instance.getClass(), e);
        }
    }
}
