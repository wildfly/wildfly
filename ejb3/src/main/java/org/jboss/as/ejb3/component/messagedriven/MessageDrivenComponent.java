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
package org.jboss.as.ejb3.component.messagedriven;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.TransactionManager;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.pool.PooledComponent;
import org.jboss.as.ejb3.inflow.JBossMessageEndpointFactory;
import org.jboss.as.ejb3.inflow.MessageEndpointService;
import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.pool.StatelessObjectFactory;
import org.jboss.as.ejb3.timerservice.PooledTimedObjectInvokerImpl;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.logging.Logger;
import org.jboss.msc.service.StopContext;

import static java.util.Collections.emptyMap;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static org.jboss.as.ejb3.component.MethodIntf.BEAN;
import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageDrivenComponent extends EJBComponent implements PooledComponent<MessageDrivenComponentInstance> {

    private final Pool<MessageDrivenComponentInstance> pool;

    private final ActivationSpec activationSpec;
    private final MessageEndpointFactory endpointFactory;
    private final Class<?> messageListenerInterface;
    private ResourceAdapter resourceAdapter;
    private final TimedObjectInvoker timedObjectInvoker;

    /**
     * Construct a new instance.
     *
     * @param ejbComponentCreateService the component configuration
     */
    protected MessageDrivenComponent(final MessageDrivenComponentCreateService ejbComponentCreateService, final Class<?> messageListenerInterface, final ActivationSpec activationSpec) {
        super(ejbComponentCreateService);

        StatelessObjectFactory<MessageDrivenComponentInstance> factory = new StatelessObjectFactory<MessageDrivenComponentInstance>() {
            @Override
            public MessageDrivenComponentInstance create() {
                return (MessageDrivenComponentInstance) createInstance();
            }

            @Override
            public void destroy(MessageDrivenComponentInstance obj) {
                obj.destroy();
            }
        };
        final PoolConfig poolConfig = ejbComponentCreateService.getPoolConfig();
        if (poolConfig == null) {
            ROOT_LOGGER.debug("Pooling is disabled for MDB " + ejbComponentCreateService.getComponentName());
            this.pool = null;
        } else {
            ROOT_LOGGER.debug("Using pool config " + poolConfig + " to create pool for MDB " + ejbComponentCreateService.getComponentName());
            this.pool = poolConfig.createPool(factory);
        }

        this.activationSpec = activationSpec;
        this.messageListenerInterface = messageListenerInterface;
        final MessageEndpointService<?> service = new MessageEndpointService<Object>() {
            @Override
            public Class<Object> getMessageListenerInterface() {
                return (Class<Object>) messageListenerInterface;
            }

            @Override
            public TransactionManager getTransactionManager() {
                return MessageDrivenComponent.this.getTransactionManager();
            }

            @Override
            public boolean isDeliveryTransacted(Method method) throws NoSuchMethodException {
                // an MDB doesn't expose a real view
                return getTransactionAttributeType(BEAN, method) == REQUIRED;
            }

            @Override
            public Object obtain(long timeout, TimeUnit unit) {
                // like this it's a disconnected invocation
//                return getComponentView(messageListenerInterface).getViewForInstance(null);
                return createViewInstanceProxy(messageListenerInterface, emptyMap());
            }

            @Override
            public void release(Object obj) {
                // do nothing
            }
        };
        this.endpointFactory = new JBossMessageEndpointFactory(getComponentClass().getClassLoader(), service);
        final String deploymentName;
        if(ejbComponentCreateService.getDistinctName() == null || ejbComponentCreateService.getDistinctName().length() == 0) {
            deploymentName = ejbComponentCreateService.getApplicationName() + "." + ejbComponentCreateService.getModuleName();
        } else {
            deploymentName = ejbComponentCreateService.getApplicationName() + "." + ejbComponentCreateService.getModuleName() + "." + ejbComponentCreateService.getDistinctName();
        }
        this.timedObjectInvoker = new PooledTimedObjectInvokerImpl(this, deploymentName);
    }

    @Override
    protected BasicComponentInstance instantiateComponentInstance(AtomicReference<ManagedReference> instanceReference, Interceptor preDestroyInterceptor, Map<Method, Interceptor> methodInterceptors, final InterceptorFactoryContext interceptorContext) {
        final Map<Method, Interceptor> timeouts;
        if (timeoutInterceptors != null) {
            timeouts = new HashMap<Method, Interceptor>();
            for (Map.Entry<Method, InterceptorFactory> entry : timeoutInterceptors.entrySet()) {
                timeouts.put(entry.getKey(), entry.getValue().create(interceptorContext));
            }
        } else {
            timeouts = Collections.emptyMap();
        }
        return new MessageDrivenComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors, timeouts);
    }

    @Override
    public Pool<MessageDrivenComponentInstance> getPool() {
        return pool;
    }

    protected void setResourceAdapter(ResourceAdapter resourceAdapter) {
        this.resourceAdapter = resourceAdapter;
    }

    @Override
    public void start() {
        if (resourceAdapter == null)
            throw MESSAGES.resourceAdapterNotSpecified(this);

        super.start();

        try {
            resourceAdapter.endpointActivation(endpointFactory, activationSpec);
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }

        if(this.pool!=null) {
            this.pool.start();
        }
    }

    @Override
    public void stop(final StopContext stopContext) {

        if(this.pool!=null){
            this.pool.stop();
        }

        resourceAdapter.endpointDeactivation(endpointFactory, activationSpec);

        super.stop(stopContext);
    }

    @Override
    public TimedObjectInvoker getTimedObjectInvoker() {
        return timedObjectInvoker;
    }
}
