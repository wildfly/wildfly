/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent;

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
public class ContextServiceImpl extends org.glassfish.enterprise.concurrent.ContextServiceImpl implements WildFlyContextService {

    private final ContextServiceTypesConfiguration contextServiceTypesConfiguration;

    /**
     *
     * @param name
     */
    public ContextServiceImpl(String name, ContextServiceTypesConfiguration contextServiceTypesConfiguration) {
        super(name, new DefaultContextSetupProviderImpl(), null);
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

    @Override
    public ContextServiceTypesConfiguration getContextServiceTypesConfiguration() {
        return contextServiceTypesConfiguration;
    }
}
