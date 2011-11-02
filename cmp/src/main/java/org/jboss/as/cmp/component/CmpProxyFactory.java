/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cmp.component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ejb.EntityBean;
import org.jboss.classfilewriter.ClassMethod;
import org.jboss.invocation.proxy.MethodBodyCreator;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.invocation.proxy.ProxyConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;

/**
 * @author John Bailey
 */
public class CmpProxyFactory extends ProxyFactory<EntityBean> {
    private static final AtomicInteger PROXY_ID = new AtomicInteger(0);

    public static ProxyFactory<EntityBean> createProxyFactory(final Class<EntityBean> beanClass, final Class<?>... viewClasses) {
        final ProxyConfiguration<EntityBean> proxyConfiguration = new ProxyConfiguration<EntityBean>();
        proxyConfiguration.setProxyName(beanClass.getName() + "$$$cmp" + PROXY_ID.incrementAndGet());
        proxyConfiguration.setClassLoader(beanClass.getClassLoader());
        proxyConfiguration.setProtectionDomain(beanClass.getProtectionDomain());
        proxyConfiguration.setSuperClass(beanClass);
        if (viewClasses != null) for (Class<?> viewClass : viewClasses) {
            if (viewClass != null) {
                proxyConfiguration.addAdditionalInterface(viewClass);
            }
        }
        proxyConfiguration.addAdditionalInterface(CmpProxy.class);
        return new CmpProxyFactory(beanClass, proxyConfiguration);
    }

    private final Class<?> beanClass;

    public CmpProxyFactory(final Class<?> beanClass, final ProxyConfiguration<EntityBean> proxyConfiguration) {
        super(proxyConfiguration);
        this.beanClass = beanClass;
    }

    protected boolean overrideMethod(final Method method, final MethodIdentifier identifier, final MethodBodyCreator creator) {
        final boolean fromComponent = method.getDeclaringClass().equals(beanClass);
        final boolean shouldOverride = fromComponent ? Modifier.isAbstract(method.getModifiers()) : !foundOnComponent(method);
        return shouldOverride && super.overrideMethod(method, identifier, creator);
    }

    protected boolean overrideMethod(final ClassMethod method, MethodIdentifier identifier, MethodBodyCreator creator) {
        final boolean fromComponent = method.getClassFile().getName().equals(beanClass.getName());
        final boolean shouldOverride = fromComponent ? Modifier.isAbstract(method.getAccessFlags()) : !foundOnComponent(method);
        return shouldOverride && super.overrideMethod(method, identifier, creator);
    }

    protected boolean foundOnComponent(final Method method) {
        return foundOnComponent(method.getName(), method.getParameterTypes());
    }

    protected boolean foundOnComponent(final ClassMethod method) {
        final String[] paramTypeNames = method.getParameters();
        final Class<?>[] paramTypes = new Class<?>[paramTypeNames.length];

        for (int i = 0; i < paramTypeNames.length; i++) {
            try {
                paramTypes[i] = beanClass.getClassLoader().loadClass(paramTypeNames[i]);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load param class for method: " + method, e);
            }
        }
        return foundOnComponent(method.getName(), paramTypes);
    }

    protected boolean foundOnComponent(final String methodName, final Class<?>[] paramTypes) {
        Class<?> current = beanClass;
        while (current != null) {
            try {
                beanClass.getDeclaredMethod(methodName, paramTypes);
                return true;
            } catch (NoSuchMethodException ignored) {
            }
            current = current.getSuperclass();
        }
        return false;
    }
}
