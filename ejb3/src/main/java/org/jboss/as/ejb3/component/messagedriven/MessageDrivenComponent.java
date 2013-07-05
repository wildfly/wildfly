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
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.pool.PooledComponent;
import org.jboss.as.ejb3.inflow.JBossMessageEndpointFactory;
import org.jboss.as.ejb3.inflow.MessageEndpointService;
import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.pool.StatelessObjectFactory;
import org.jboss.as.naming.ManagedReference;
import org.wildfly.security.manager.GetClassLoaderAction;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.jca.core.spi.rar.Endpoint;
import org.wildfly.security.manager.WildFlySecurityManager;

import static java.security.AccessController.doPrivileged;
import static java.util.Collections.emptyMap;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.component.MethodIntf.BEAN;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageDrivenComponent extends EJBComponent implements PooledComponent<MessageDrivenComponentInstance> {

    private final Pool<MessageDrivenComponentInstance> pool;
    private final String poolName;

    private final ActivationSpec activationSpec;
    private final MessageEndpointFactory endpointFactory;
    private final Class<?> messageListenerInterface;
    private final ClassLoader classLoader;
    private volatile boolean deliveryActive;
    private ResourceAdapter resourceAdapter;
    private Endpoint endpoint;

    /**
     * Construct a new instance.
     *
     * @param ejbComponentCreateService the component configuration
     * @param deliveryActive true if the component must start delivering messages as soon as it is started
     */
    protected MessageDrivenComponent(final MessageDrivenComponentCreateService ejbComponentCreateService, final Class<?> messageListenerInterface, final ActivationSpec activationSpec, final boolean deliveryActive) {
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
            this.poolName = null;
        } else {
            ROOT_LOGGER.debug("Using pool config " + poolConfig + " to create pool for MDB " + ejbComponentCreateService.getComponentName());
            this.pool = poolConfig.createPool(factory);
            this.poolName = poolConfig.getPoolName();
        }
        this.classLoader = ejbComponentCreateService.getModuleClassLoader();

        this.activationSpec = activationSpec;
        this.messageListenerInterface = messageListenerInterface;
        final ClassLoader componentClassLoader = doPrivileged(new GetClassLoaderAction(ejbComponentCreateService.getComponentClass()));
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
                if(isBeanManagedTransaction())
                    return false;
                // an MDB doesn't expose a real view
                return getTransactionAttributeType(BEAN, method) == REQUIRED;
            }

            @Override
            public Object obtain(long timeout, TimeUnit unit) {
                // like this it's a disconnected invocation
//                return getComponentView(messageListenerInterface).getViewForInstance(null);
                return createViewInstanceProxy(getComponentClass(), emptyMap());
            }

            @Override
            public void release(Object obj) {
                // do nothing
            }

            @Override
            public ClassLoader getClassLoader() {
                return componentClassLoader;
            }
        };
        this.endpointFactory = new JBossMessageEndpointFactory(componentClassLoader, service, (Class<Object>) getComponentClass());
        this.deliveryActive = deliveryActive;
    }

    @Override
    protected BasicComponentInstance instantiateComponentInstance(AtomicReference<ManagedReference> instanceReference, Interceptor preDestroyInterceptor, Map<Method, Interceptor> methodInterceptors, final InterceptorFactoryContext interceptorContext) {
        return new MessageDrivenComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors);
    }

    @Override
    public Pool<MessageDrivenComponentInstance> getPool() {
        return pool;
    }

    @Override
    public String getPoolName() {
        return poolName;
    }

    protected void setResourceAdapter(ResourceAdapter resourceAdapter) {
        this.resourceAdapter = resourceAdapter;
    }

    void setEndpoint(final Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void start() {
        if (endpoint == null) {
            throw MESSAGES.endpointUnAvailable(this.getComponentName());
        }

        getShutDownInterceptorFactory().start();
        super.start();

        if (deliveryActive) {
            activate();
        }

        if (this.pool != null) {
            this.pool.start();
        }
    }

    @Override
    public void stop() {

        deactivate();
        deliveryActive = false;

        getShutDownInterceptorFactory().shutdown();
        if (this.pool != null) {
            this.pool.stop();
        }

        super.stop();
    }

    private void activate() {
        ClassLoader oldTccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);

            this.endpoint.activate(endpointFactory, activationSpec);
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
        }
    }

    private void deactivate() {
        ClassLoader oldTccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            endpoint.deactivate(endpointFactory, activationSpec);
        } catch (ResourceException re) {
            throw MESSAGES.failureDuringEndpointDeactivation(this.getComponentName(), re);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
        }
    }

    public void startDelivery() {
        this.deliveryActive = true;
        activate();
    }

    public void stopDelivery() {
        this.deactivate();
        this.deliveryActive = false;
    }

    public boolean isDeliveryActive() {
        return deliveryActive;
    }

    @Override
    public AllowedMethodsInformation getAllowedMethodsInformation() {
        return isBeanManagedTransaction() ? MessageDrivenAllowedMethodsInformation.INSTANCE_BMT : MessageDrivenAllowedMethodsInformation.INSTANCE_CMT;
    }
}
