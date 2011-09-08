/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.component;

import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.marshalling.cloner.ClassLoaderClassCloner;
import org.jboss.marshalling.cloner.ClonerConfiguration;
import org.jboss.marshalling.cloner.ObjectCloner;
import org.jboss.marshalling.cloner.ObjectClonerFactory;
import org.jboss.marshalling.cloner.ObjectCloners;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class MarshallingInvocationHandler implements InvocationHandler {
    private final IdentityHashMap<Method, Method> methodMap;
    private final Object original;
    private final ObjectCloner unwrapper;
    private final ObjectCloner wrapper;

    MarshallingInvocationHandler(final ProxyFactory<?> outerProxyFactory, final ProxyFactory<?> innerProxyFactory, final Object original) {
        this.methodMap = new IdentityHashMap<Method, Method>();
        for (final Method innerMethod : innerProxyFactory.getCachedMethods()) {
            methodMap.put(outerMethod(outerProxyFactory.getCachedMethods(), innerMethod), innerMethod);
        }
        this.original = original;
        final ObjectClonerFactory factory = ObjectCloners.getSerializingObjectClonerFactory();
        this.unwrapper = factory.createCloner(clonerConfiguration(outerProxyFactory.getClassLoader()));
        this.wrapper = factory.createCloner(clonerConfiguration(original.getClass().getClassLoader()));
    }

    private static boolean arrayNameEquals(final Class<?>[] a, final Class<?>[] a2) {
        int length = a.length;
        if (a2.length != length)
            return false;

        for (int i = 0; i < length; i++) {
            String o1 = a[i].getName();
            String o2 = a2[i].getName();
            if (!(o1 == null ? o2 == null : o1.equals(o2)))
                return false;
        }
        return true;
    }

    private static ClonerConfiguration clonerConfiguration(final ClassLoader destClassLoader) {
        final ClonerConfiguration configuration = new ClonerConfiguration();
        configuration.setClassCloner(new ClassLoaderClassCloner(destClassLoader));
        return configuration;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final Object[] originalMethodArgs = (Object[]) wrap(args);
        final Method originalMethod = methodMap.get(method);
        // TODO: skip reflect, go straight to the invocation handler
        final Object result = originalMethod.invoke(original, originalMethodArgs);
        return unwrap(result);
    }

    private static Method outerMethod(final List<Method> cachedMethods, final Method innerMethod) {
        for (final Method outerMethod : cachedMethods) {
            if (!outerMethod.getName().equals(innerMethod.getName()))
                continue;
            if (!arrayNameEquals(outerMethod.getParameterTypes(), innerMethod.getParameterTypes()))
                continue;
            return outerMethod;
        }
        throw new RuntimeException("Can't find a method for " + innerMethod + " out of " + cachedMethods);
    }

    private Object wrap(final Object value) {
        try {
            return wrapper.clone(value);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Object unwrap(final Object value) {
        try {
            return unwrapper.clone(value);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
