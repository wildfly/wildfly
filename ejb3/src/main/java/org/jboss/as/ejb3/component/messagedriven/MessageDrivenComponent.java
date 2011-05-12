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

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.EJBComponentCreateService;
import org.jboss.as.ejb3.component.pool.PooledComponent;
import org.jboss.as.ejb3.inflow.JBossMessageEndpointFactory;
import org.jboss.as.ejb3.inflow.MessageEndpointService;
import org.jboss.as.naming.ManagedReference;
import org.jboss.ejb3.context.spi.MessageDrivenBeanComponent;
import org.jboss.ejb3.pool.Pool;
import org.jboss.ejb3.pool.StatelessObjectFactory;
import org.jboss.ejb3.pool.strictmax.StrictMaxPool;
import org.jboss.invocation.Interceptor;
import org.jboss.msc.service.StopContext;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.TransactionManager;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static javax.ejb.TransactionAttributeType.REQUIRED;
import static org.jboss.as.ejb3.component.MethodIntf.BEAN;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageDrivenComponent extends EJBComponent implements MessageDrivenBeanComponent, PooledComponent<MessageDrivenComponentInstance> {
    private final Pool<MessageDrivenComponentInstance> pool;

    // TODO: implement creation of ActivationSpec
    private final ActivationSpec activationSpec = null;
    private final MessageEndpointFactory endpointFactory;
    private final Class<?> messageListenerInterface;
    private ResourceAdapter resourceAdapter;

    /**
     * Construct a new instance.
     *
     * @param ejbComponentCreateService the component configuration
     */
    protected MessageDrivenComponent(final EJBComponentCreateService ejbComponentCreateService) {
        super(ejbComponentCreateService);

        StatelessObjectFactory<MessageDrivenComponentInstance> factory = new StatelessObjectFactory<MessageDrivenComponentInstance>() {
            @Override
            public MessageDrivenComponentInstance create() {
                return (MessageDrivenComponentInstance) createInstance();
            }

            @Override
            public void destroy(MessageDrivenComponentInstance obj) {
                throw new RuntimeException("NYI");
                //destroyInstance(obj);
            }
        };
        this.pool = new StrictMaxPool<MessageDrivenComponentInstance>(factory, 20, 5, TimeUnit.MINUTES);

        this.messageListenerInterface = null; //ejbComponentCreateService.getMessageListenerInterface();
        final MessageEndpointService<?> service = new MessageEndpointService<Object>() {
            @Override
            public Class<Object> getMessageListenerInterface() {
                return (Class<Object>) messageListenerInterface;
            }

            @Override
            public TransactionManager getTransactionManager() {
                return getTransactionManager();
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
                throw new RuntimeException("NYI");
            }

            @Override
            public void release(Object obj) {
                // do nothing
            }
        };
        this.endpointFactory = new JBossMessageEndpointFactory(service);
    }

    @Override
    protected BasicComponentInstance instantiateComponentInstance(AtomicReference<ManagedReference> instanceReference, Interceptor preDestroyInterceptor, Map<Method, Interceptor> methodInterceptors) {
        return new MessageDrivenComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors);
    }

//    @Override
//    public Interceptor createClientInterceptor(Class<?> view) {
//        return createClientInterceptor(view, null);
//    }
//
//    @Override
//    public Interceptor createClientInterceptor(Class<?> view, Serializable sessionId) {
//        return new Interceptor() {
//            @Override
//            public Object processInvocation(InterceptorContext context) throws Exception {
//                // TODO: FIXME: Component shouldn't be attached in a interceptor context that
//                // runs on remote clients.
//                context.putPrivateData(Component.class, MessageDrivenComponent.this);
//                try {
//                    return context.proceed();
//                }
//                finally {
//                    context.putPrivateData(Component.class, null);
//                }
//            }
//        };
//    }

    @Override
    public Pool<MessageDrivenComponentInstance> getPool() {
        return pool;
    }

    protected void setResourceAdapter(ResourceAdapter resourceAdapter) {
        this.resourceAdapter = resourceAdapter;
    }

    @Override
    public void start() {
        super.start();

        try {
            resourceAdapter.endpointActivation(endpointFactory, activationSpec);
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop(final StopContext stopContext) {
        resourceAdapter.endpointDeactivation(endpointFactory, activationSpec);

        super.stop(stopContext);
    }
}
