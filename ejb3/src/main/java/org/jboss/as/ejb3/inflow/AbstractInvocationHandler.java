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
package org.jboss.as.ejb3.inflow;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
abstract class AbstractInvocationHandler implements InvocationHandler {
    protected abstract boolean doEquals(Object obj);

    protected abstract Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable;

    @Override
    public final boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null)
            return false;

        if (Proxy.isProxyClass(obj.getClass()))
            return equals(Proxy.getInvocationHandler(obj));

        // It might not be a JDK proxy that's handling this, so here we do a little trick.
        // Normally you would do:
        //      if(!(obj instanceof EndpointInvocationHandler))
        //         return false;
        if (!(obj instanceof AbstractInvocationHandler))
            return obj.equals(this);

        return doEquals(obj);
    }

    protected Object handle(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(this, args);
        }
        catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public abstract int hashCode();

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass().equals(Object.class))
            return handle(method, args);
        return doInvoke(proxy, method, args);
    }
}
