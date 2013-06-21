/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.as.concurrent.context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * A {@link InvocationHandler} which invocations are done with a specific {@link Context} set.
 *
 * @author Eduardo Martins
 */
public class ContextualProxyInvocationHandler implements InvocationHandler {

    private final Map<String, String> executionProperties;
    private final Context context;
    private final Object instance;

    public ContextualProxyInvocationHandler(Map<String, String> executionProperties, Context context, Object instance) {
        this.executionProperties = executionProperties;
        this.context = context;
        this.instance = instance;
    }

    public Map<String, String> getExecutionProperties() {
        return executionProperties;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == java.lang.Object.class) {
            // Object methods are invoked without any context
            return method.invoke(instance, args);
        }
        final Context previousContext = Utils.setContext(context);
        try {
            return method.invoke(instance, args);
        } finally {
            Utils.setContext(previousContext);
        }
    }

}
