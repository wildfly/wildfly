/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.component.entity;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.entity.entitycache.ReadyEntityCache;
import org.jboss.as.ejb3.component.entity.entitycache.TransactionLocalEntityCache;
import org.jboss.as.ejb3.pool.InfinitePool;
import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.pool.StatelessObjectFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * @author Stuart Douglas
 */
public class EntityBeanComponent extends EJBComponent {

    public static final Object PRIMARY_KEY_CONTEXT_KEY = new Object();

    private final Pool<EntityBeanComponentInstance> pool;
    private final ReadyEntityCache cache;

    protected EntityBeanComponent(final EntityBeanComponentCreateService ejbComponentCreateService) {
        super(ejbComponentCreateService);

        StatelessObjectFactory<EntityBeanComponentInstance> factory = new StatelessObjectFactory<EntityBeanComponentInstance>() {
            @Override
            public EntityBeanComponentInstance create() {
                return (EntityBeanComponentInstance) createInstance();
            }

            @Override
            public void destroy(EntityBeanComponentInstance obj) {
                obj.destroy();
            }
        };
        pool = new InfinitePool<EntityBeanComponentInstance>(factory);
        cache = new TransactionLocalEntityCache(this);
    }



    @Override
    protected BasicComponentInstance instantiateComponentInstance(final AtomicReference<ManagedReference> instanceReference, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors, final InterceptorFactoryContext interceptorContext) {
        final EntityBeanComponentInstance instance =  new EntityBeanComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors);
        return instance;
    }

    public ReadyEntityCache getCache() {
        return cache;
    }

    public Pool<EntityBeanComponentInstance> getPool() {
        return pool;
    }
}
