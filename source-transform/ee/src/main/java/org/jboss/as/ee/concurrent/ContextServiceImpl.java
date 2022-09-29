/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ee.concurrent;

import org.glassfish.enterprise.concurrent.spi.ContextSetupProvider;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;
import static org.wildfly.common.Assert.checkArrayBounds;
import static org.wildfly.common.Assert.checkNotNullParam;

/**
 * An extension of Jakarta EE RI {@link org.glassfish.enterprise.concurrent.ContextServiceImpl}, which properly supports a security manager.
 * @author Eduardo Martins
 */
public class ContextServiceImpl extends org.glassfish.enterprise.concurrent.ContextServiceImpl {

    private final ContextServiceTypesConfiguration contextServiceTypesConfiguration;

    /**
     *
     * @param name
     * @param contextSetupProvider
     */
    public ContextServiceImpl(String name, ContextSetupProvider contextSetupProvider, ContextServiceTypesConfiguration contextServiceTypesConfiguration) {
        super(name, contextSetupProvider, null);
        this.contextServiceTypesConfiguration = contextServiceTypesConfiguration;
    }

    private <T> T internalCreateContextualProxy(T instance, Map<String, String> executionProperties, Class<T> intf) {
        checkNotNullParam("instance", instance);
        checkNotNullParam("intf", intf);

        IdentityAwareProxyInvocationHandler handler = new IdentityAwareProxyInvocationHandler(this, instance, executionProperties);
        Object proxy = Proxy.newProxyInstance(instance.getClass().getClassLoader(), new Class[]{intf}, handler);
        return  intf.cast(proxy);
    }

    @Override
    public <T> T createContextualProxy(final T instance, final Map<String, String> executionProperties, final Class<T> intf) {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(new PrivilegedAction<T>() {
                @Override
                public T run() {
                    return internalCreateContextualProxy(instance, executionProperties, intf);
                }
            });
        } else {
            return internalCreateContextualProxy(instance, executionProperties, intf);
        }
    }

    private Object internalCreateContextualProxy(Object instance, Map<String, String> executionProperties,
            Class<?>... interfaces) {
        checkNotNullParam("instance", instance);
        checkArrayBounds(checkNotNullParam("interfaces", interfaces), 0, 1);

        Class<? extends Object> instanceClass = instance.getClass();
        for (Class<? extends Object> thisInterface : interfaces) {
            if (!thisInterface.isAssignableFrom(instanceClass)) {
                throw ROOT_LOGGER.classDoesNotImplementAllInterfaces();
            }
        }
        IdentityAwareProxyInvocationHandler handler = new IdentityAwareProxyInvocationHandler(this, instance, executionProperties);
        Object proxy = Proxy.newProxyInstance(instance.getClass().getClassLoader(), interfaces, handler);
        return proxy;
    }

    @Override
    public Object createContextualProxy(final Object instance, final Map<String, String> executionProperties, final Class<?>... interfaces) {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    return internalCreateContextualProxy(instance, executionProperties, interfaces);
                }
            });
        } else {
            return internalCreateContextualProxy(instance, executionProperties, interfaces);
        }
    }

    public ContextServiceTypesConfiguration getContextServiceTypesConfiguration() {
        return contextServiceTypesConfiguration;
    }

    // TODO *FOLLOW UP* revisit RI impl of the async methods, which quality seems to have issues (e.g. each method uses a new Managed Executor instance...)
}
