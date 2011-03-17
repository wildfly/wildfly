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

import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.pool.PooledComponent;
import org.jboss.ejb3.context.spi.MessageDrivenBeanComponent;
import org.jboss.ejb3.pool.Pool;
import org.jboss.ejb3.pool.StatelessObjectFactory;
import org.jboss.ejb3.pool.strictmax.StrictMaxPool;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactoryContext;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageDrivenComponent extends EJBComponent implements MessageDrivenBeanComponent, PooledComponent<MessageDrivenComponentInstance> {
    private final Pool<MessageDrivenComponentInstance> pool;

    /**
     * Construct a new instance.
     *
     * @param configuration the component configuration
     */
    protected MessageDrivenComponent(final MessageDrivenComponentConfiguration configuration) {
        super(configuration);

        StatelessObjectFactory<MessageDrivenComponentInstance> factory = new StatelessObjectFactory<MessageDrivenComponentInstance>() {
            @Override
            public MessageDrivenComponentInstance create() {
                return (MessageDrivenComponentInstance) createInstance();
            }

            @Override
            public void destroy(MessageDrivenComponentInstance obj) {
                destroyInstance(obj);
            }
        };
        this.pool = new StrictMaxPool<MessageDrivenComponentInstance>(factory, 20, 5, TimeUnit.MINUTES);
    }

    @Override
    protected MessageDrivenComponentInstance constructComponentInstance(Object instance, List<Interceptor> preDestroyInterceptors, InterceptorFactoryContext context) {
        return new MessageDrivenComponentInstance(this, instance, preDestroyInterceptors, context);
    }

    @Override
    public Interceptor createClientInterceptor(Class<?> view) {
        return createClientInterceptor(view, null);
    }

    @Override
    public Interceptor createClientInterceptor(Class<?> view, Serializable sessionId) {
        return new Interceptor() {
            @Override
            public Object processInvocation(InterceptorContext context) throws Exception {
                // TODO: FIXME: Component shouldn't be attached in a interceptor context that
                // runs on remote clients.
                context.putPrivateData(Component.class, MessageDrivenComponent.this);
                try {
                    return context.proceed();
                }
                finally {
                    context.putPrivateData(Component.class, null);
                }
            }
        };
    }

    @Override
    public Pool<MessageDrivenComponentInstance> getPool() {
        return pool;
    }
}
