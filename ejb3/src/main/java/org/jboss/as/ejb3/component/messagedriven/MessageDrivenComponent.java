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

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.TransactionManager;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.pool.PooledComponent;
import org.jboss.as.ejb3.inflow.JBossMessageEndpointFactory;
import org.jboss.as.ejb3.inflow.MessageEndpointService;
import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.pool.StatelessObjectFactory;
import org.jboss.as.server.suspend.ServerActivity;
import org.jboss.as.server.suspend.ServerActivityCallback;
import org.jboss.as.server.suspend.SuspendController;
import org.wildfly.security.manager.action.GetClassLoaderAction;
import org.jboss.invocation.Interceptor;
import org.jboss.jca.core.spi.rar.Endpoint;
import org.wildfly.security.manager.WildFlySecurityManager;

import static java.security.AccessController.doPrivileged;
import static java.util.Collections.emptyMap;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

import static org.jboss.as.ejb3.component.MethodIntf.MESSAGE_ENDPOINT;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageDrivenComponent extends EJBComponent implements PooledComponent<MessageDrivenComponentInstance> {

    private final Pool<MessageDrivenComponentInstance> pool;
    private final String poolName;

    private final SuspendController suspendController;
    private final ActivationSpec activationSpec;
    private final MessageEndpointFactory endpointFactory;
    private final ClassLoader classLoader;
    private volatile boolean deliveryActive;
    private Endpoint endpoint;
    private String activationName;

    /**
     * Server activity that stops delivery before suspend starts.
     *
     * Note that there is a very small (but unavoidable) race here. It is possible
     * that a message will have started delivery when preSuspend is called, but has
     * not make it to the {@link org.jboss.as.ejb3.deployment.processors.EjbSuspendInterceptor},
     * if this happens the message will be rejected with an exception.
     *
     */
    private final ServerActivity serverActivity = new ServerActivity() {
        @Override
        public void preSuspend(ServerActivityCallback listener) {
            if(deliveryActive) {
                deactivate();
            }
            listener.done();
        }

        public void suspended(ServerActivityCallback listener) {
            listener.done();
        }

        @Override
        public void resume() {
            if(deliveryActive) {
                activate();
            }
        }
    };

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
            ROOT_LOGGER.debugf("Pooling is disabled for MDB %s", ejbComponentCreateService.getComponentName());
            this.pool = null;
            this.poolName = null;
        } else {
            ROOT_LOGGER.debugf("Using pool config %s to create pool for MDB %s", poolConfig, ejbComponentCreateService.getComponentName());
            this.pool = poolConfig.createPool(factory);
            this.poolName = poolConfig.getPoolName();
        }
        this.classLoader = ejbComponentCreateService.getModuleClassLoader();
        this.suspendController = ejbComponentCreateService.getSuspendControllerInjectedValue().getValue();
        this.activationSpec = activationSpec;
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
                return getTransactionAttributeType(MESSAGE_ENDPOINT, method) == REQUIRED;
            }

            @Override
            public String getActivationName() {
                return activationName;
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
        this.endpointFactory = new JBossMessageEndpointFactory(componentClassLoader, service, (Class<Object>) getComponentClass(), messageListenerInterface);
        this.deliveryActive = deliveryActive;
    }

    @Override
    protected BasicComponentInstance instantiateComponentInstance(Interceptor preDestroyInterceptor, Map<Method, Interceptor> methodInterceptors, Map<Object, Object> context) {
        return new MessageDrivenComponentInstance(this, preDestroyInterceptor, methodInterceptors);
    }

    @Override
    public Pool<MessageDrivenComponentInstance> getPool() {
        return pool;
    }

    @Override
    public String getPoolName() {
        return poolName;
    }

    void setEndpoint(final Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void start() {
        if (endpoint == null) {
            throw EjbLogger.ROOT_LOGGER.endpointUnAvailable(this.getComponentName());
        }

        super.start();

        if (deliveryActive) {
            activate();
        }

        if (this.pool != null) {
            this.pool.start();
        }
        suspendController.registerActivity(serverActivity);

    }

    @Override
    public void done() {

        deactivate();

        if (this.pool != null) {
            this.pool.stop();
        }

        suspendController.unRegisterActivity(serverActivity);
        super.done();
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
            throw EjbLogger.ROOT_LOGGER.failureDuringEndpointDeactivation(this.getComponentName(), re);
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
