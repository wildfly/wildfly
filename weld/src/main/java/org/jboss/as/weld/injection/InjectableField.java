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

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.jboss.as.weld.WeldMessages;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * tracks fields to be injected
 */
final class InjectableField {
    private final Field field;
    private final Bean<?> bean;
    private final FieldInjectionPoint injectionPoint;

    public InjectableField(final Field field, final Bean<?> bean, final FieldInjectionPoint injectionPoint) {
        this.bean = bean;
        this.field = field;
        this.injectionPoint = injectionPoint;
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                field.setAccessible(true);
                return null;
            }
        });
    }

    /**
     * Injects into a field injection point
     *
     * @param instance    The instance to inject
     * @param beanManager The current BeanManager
     * @param ctx         The creational context to use
     */
    public void inject(Object instance, BeanManagerImpl beanManager, CreationalContext<?> ctx) {
        try {
            final Object value = beanManager.getReference(injectionPoint, bean, ctx);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw WeldMessages.MESSAGES.couldNotInjectField(field, instance.getClass(), e);
        }
    }
}
