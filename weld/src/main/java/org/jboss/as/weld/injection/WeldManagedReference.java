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

import java.io.Serializable;
import java.util.Map;

import javax.enterprise.context.spi.CreationalContext;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.weld.WeldMessages;

/**
* @author Stuart Douglas
*/
class WeldManagedReference implements ManagedReference, Serializable {
    private final CreationalContext<?> context;
    private final Object instance;

    //the following fields are transient, as they are only needed at creation time,
    //and should not be needed after injection is complete
    private final transient WeldEEInjection injectionTarget;
    private final transient Map<Class<?>, WeldEEInjection> interceptorInjections;

    public WeldManagedReference(CreationalContext<?> ctx, Object instance, final WeldEEInjection injectionTarget, final Map<Class<?>, WeldEEInjection> interceptorInjections) {
        this.context = ctx;
        this.instance = instance;
        this.injectionTarget = injectionTarget;
        this.interceptorInjections = interceptorInjections;
    }

    /**
     * Runs CDI injection on the instance. This should be called after resource injection has been performed
     */
    public void inject() {
        injectionTarget.inject(instance, context);
    }

    public void injectInterceptor(Class<?> interceptorClass, Object instance) {
        final WeldEEInjection injection = interceptorInjections.get(interceptorClass);
        if(injection != null) {
            injection.inject(instance, context);
        } else {
            throw WeldMessages.MESSAGES.unknownInterceptorClassForCDIInjection(interceptorClass);
        }
    }

    @Override
    public void release() {
        context.release();
    }

    @Override
    public Object getInstance() {
        return instance;
    }

    public CreationalContext<?> getContext() {
        return context;
    }

    public WeldEEInjection getInjectionTarget() {
        return injectionTarget;
    }
}
