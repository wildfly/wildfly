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

package org.wildfly.as.concurrent;

import org.wildfly.as.concurrent.context.Context;
import org.wildfly.as.concurrent.context.ContextConfiguration;
import org.wildfly.as.concurrent.context.ContextualProxyInvocationHandler;

import javax.enterprise.concurrent.ContextService;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * Impl for {@link ContextService}
 *
 * @author Eduardo Martins
 */
public class ContextServiceImpl implements ContextService {

    private final ContextConfiguration contextConfiguration;

    public ContextServiceImpl(ContextConfiguration contextConfiguration) {
        this.contextConfiguration = contextConfiguration;
    }

    @Override
    public Object createContextualProperty(Object instance, Class<?>... interfaces) throws IllegalArgumentException {
        return createContextualProperty(instance, null, interfaces);
    }

    @Override
    public Object createContextualProperty(Object instance, Map<String, String> executionProperties, Class<?>... interfaces) throws IllegalArgumentException {
        return Proxy.newProxyInstance(instance.getClass().getClassLoader(), interfaces, new ContextualProxyInvocationHandler(executionProperties, newContextualProxyContext(instance), instance));
    }

    private Context newContextualProxyContext(Object instance) {
        return contextConfiguration != null ? contextConfiguration.newContextualProxyContext(instance) : null;
    }

    @Override
    public <T> T createContextualProperty(T instance, Class<T> _interface) throws IllegalArgumentException {
        return createContextualProperty(instance, null, _interface);
    }

    @Override
    public <T> T createContextualProperty(T instance, Map<String, String> executionProperties, Class<T> _interface) throws IllegalArgumentException {
        return (T) Proxy.newProxyInstance(instance.getClass().getClassLoader(), new Class[]{_interface}, new ContextualProxyInvocationHandler(executionProperties, newContextualProxyContext(instance), instance));
    }

    @Override
    public Map<String, String> getExecutionProperties(Object contextualProxy) throws IllegalArgumentException {
        InvocationHandler invocationHandler = Proxy.getInvocationHandler(contextualProxy);
        if (invocationHandler instanceof ContextualProxyInvocationHandler) {
            return ((ContextualProxyInvocationHandler) invocationHandler).getExecutionProperties();
        }
        throw ConcurrentMessages.MESSAGES.unexpectedInvocationHandlerType(invocationHandler);
    }

}
