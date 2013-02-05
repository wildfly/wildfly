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

import org.jboss.invocation.proxy.ProxyConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;

import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class JBossMessageEndpointFactory implements MessageEndpointFactory {
    private static final AtomicInteger PROXY_ID = new AtomicInteger(0);
    private final MessageEndpointService service;
    private final ProxyFactory<Object> factory;

    public JBossMessageEndpointFactory(final ClassLoader classLoader, final MessageEndpointService service, final Class<Object> ejbClass) {
        this.service = service;
        final ProxyConfiguration<Object> configuration = new ProxyConfiguration<Object>()
                .setClassLoader(classLoader)
                .setProxyName(ejbClass.getName() + "$$$endpoint" + PROXY_ID.incrementAndGet())
                .setSuperClass(ejbClass)
                .addAdditionalInterface(MessageEndpoint.class);
        this.factory = new ProxyFactory<Object>(configuration);
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource) throws UnavailableException {
        return createEndpoint(xaResource, 0);
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException {
        Object delegate = service.obtain(timeout, MILLISECONDS);
        MessageEndpointInvocationHandler handler = new MessageEndpointInvocationHandler(service, delegate, xaResource);
        try {
            return (MessageEndpoint) factory.newInstance(handler);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isDeliveryTransacted(Method method) throws NoSuchMethodException {
        return service.isDeliveryTransacted(method);
    }
}
