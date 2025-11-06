/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;
import static org.wildfly.common.Assert.checkArrayBounds;
import static org.wildfly.common.Assert.checkNotNullParam;

/**
 * An extension of {@link org.glassfish.concurro.ContextServiceImpl}.
 * @author Eduardo Martins
 */
public class ConcurroContextServiceImpl extends org.glassfish.concurro.ContextServiceImpl implements WildFlyContextService {

    private final ContextServiceTypesConfiguration contextServiceTypesConfiguration;

    /**
     *
     * @param name
     */
    public ConcurroContextServiceImpl(String name, ContextServiceTypesConfiguration contextServiceTypesConfiguration) {
        super(name, new ConcurroDefaultContextSetupProviderImpl(), null);
        this.contextServiceTypesConfiguration = contextServiceTypesConfiguration;
    }

    @Override
    public <T> T createContextualProxy(final T instance, final Map<String, String> executionProperties, final Class<T> intf) {
        checkNotNullParam("instance", instance);
        checkNotNullParam("intf", intf);
        ConcurroIdentityAwareProxyInvocationHandler handler = new ConcurroIdentityAwareProxyInvocationHandler(this, instance, executionProperties);
        Object proxy = Proxy.newProxyInstance(instance.getClass().getClassLoader(), new Class[]{intf}, handler);
        return  intf.cast(proxy);
    }

    @Override
    public Object createContextualProxy(final Object instance, final Map<String, String> executionProperties, final Class<?>... interfaces) {
        checkNotNullParam("instance", instance);
        checkArrayBounds(checkNotNullParam("interfaces", interfaces), 0, 1);
        Class<? extends Object> instanceClass = instance.getClass();
        for (Class<? extends Object> thisInterface : interfaces) {
            if (!thisInterface.isAssignableFrom(instanceClass)) {
                throw ROOT_LOGGER.classDoesNotImplementAllInterfaces();
            }
        }
        ConcurroIdentityAwareProxyInvocationHandler handler = new ConcurroIdentityAwareProxyInvocationHandler(this, instance, executionProperties);
        Object proxy = Proxy.newProxyInstance(instance.getClass().getClassLoader(), interfaces, handler);
        return proxy;
    }

    @Override
    public ContextServiceTypesConfiguration getContextServiceTypesConfiguration() {
        return contextServiceTypesConfiguration;
    }
}
