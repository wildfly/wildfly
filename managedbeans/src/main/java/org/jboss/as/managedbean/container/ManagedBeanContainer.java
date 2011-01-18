/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.managedbean.container;

import org.jboss.as.ee.container.AbstractBeanContainer;
import org.jboss.as.ee.container.injection.ResourceInjection;
import org.jboss.as.ee.container.interceptor.LifecycleInterceptor;
import org.jboss.as.ee.container.interceptor.MethodInterceptor;

import java.util.List;

/**
 * Implementation of {@link org.jboss.as.ee.container.BeanContainer} used to managed instances of managed beans.
 *
 * @param <T> The managed bean object type
 *
 * @author John E. Bailey
 */
public class ManagedBeanContainer<T> extends AbstractBeanContainer<T> {
    public ManagedBeanContainer(final Class<T> beanClass, final ClassLoader beanClassLoader, final List<ResourceInjection> resourceInjections,
        final List<LifecycleInterceptor> postConstrucInterceptors, final List<LifecycleInterceptor> preDestroyInterceptors, final List<MethodInterceptor> methodInterceptors) {

        super(beanClass, beanClassLoader, resourceInjections, postConstrucInterceptors, preDestroyInterceptors, methodInterceptors);
    }
}
