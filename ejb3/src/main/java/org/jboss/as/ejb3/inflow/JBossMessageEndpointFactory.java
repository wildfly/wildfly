/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.inflow;

import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.resource.spi.UnavailableException;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.jboss.as.server.deployment.ModuleClassFactory;
import org.jboss.invocation.proxy.ProxyConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

import static java.security.AccessController.doPrivileged;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class JBossMessageEndpointFactory implements MessageEndpointFactory {
    private static final AtomicInteger PROXY_ID = new AtomicInteger(0);
    private final MessageEndpointService<?> service;
    private final ProxyFactory<Object> factory;
    private final Class<?> endpointClass;

    public JBossMessageEndpointFactory(final ClassLoader classLoader, final MessageEndpointService<?> service, final Class<Object> ejbClass, final Class<?> messageListenerInterface) {
        // todo: generics bug; only Object.class is a Class<Object>.  Everything else is Class<? extends Object> aka Class<?>
        this.service = service;
        final ProxyConfiguration<Object> configuration = new ProxyConfiguration<Object>()
                .setClassLoader(classLoader)
                .setProxyName(ejbClass.getName() + "$$$endpoint" + PROXY_ID.incrementAndGet())
                .setSuperClass(ejbClass)
                .setClassFactory(ModuleClassFactory.INSTANCE)
                .setProtectionDomain(ejbClass.getProtectionDomain())
                .addAdditionalInterface(MessageEndpoint.class)
                .addAdditionalInterface(messageListenerInterface);
        this.factory = new ProxyFactory<Object>(configuration);
        this.endpointClass = ejbClass;
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource) throws UnavailableException {
        return createEndpoint(xaResource, 0);
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException {
        Object delegate = service.obtain(timeout, MILLISECONDS);
        MessageEndpointInvocationHandler handler = new MessageEndpointInvocationHandler(service, delegate, xaResource);
        // New instance creation leads to component initialization which needs to have the TCCL that corresponds to the
        // component classloader. @see https://issues.jboss.org/browse/WFLY-3989
        final ClassLoader oldTCCL = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();

        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(this.factory.getClassLoader());

            if (System.getSecurityManager() == null) {
                return createEndpoint(factory, handler);
            } else {
                return doPrivileged((PrivilegedAction<MessageEndpoint>) () -> {
                    return createEndpoint(factory, handler);
                });
            }

        } finally {
            // reset TCCL
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTCCL);
        }

    }

    @Override
    public boolean isDeliveryTransacted(Method method) throws NoSuchMethodException {
        return service.isDeliveryTransacted(method);
    }

    @Override
    public String getActivationName() {
        return service.getActivationName();
    }

    @Override
    public Class<?> getEndpointClass() {
        // The MDB class is the message endpoint class
        return this.endpointClass;
    }

    private MessageEndpoint createEndpoint(ProxyFactory<Object> factory, MessageEndpointInvocationHandler handler) {
        try {
            return (MessageEndpoint) factory.newInstance(handler);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
