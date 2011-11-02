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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.entity.entitycache.ReadyEntityCache;
import org.jboss.as.ejb3.component.entity.entitycache.ReferenceCountingEntityCache;
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
    private final Class<EJBHome> homeClass;
    private final Class<EJBLocalHome> localHomeClass;
    private final Class<EJBLocalObject> localClass;
    private final Class<EJBObject> remoteClass;

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
        this.cache = createEntityCache(ejbComponentCreateService);

        this.homeClass = ejbComponentCreateService.getHomeClass();
        this.localHomeClass = ejbComponentCreateService.getLocalHomeClass();
        this.localClass = ejbComponentCreateService.getLocalClass();
        this.remoteClass = ejbComponentCreateService.getRemoteClass();
    }

    @Override
    protected BasicComponentInstance instantiateComponentInstance(final AtomicReference<ManagedReference> instanceReference, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors, final InterceptorFactoryContext interceptorContext) {
        return new EntityBeanComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors);
    }

    public ReadyEntityCache getCache() {
        return cache;
    }

    public Pool<EntityBeanComponentInstance> getPool() {
        return pool;
    }

    protected ReadyEntityCache createEntityCache(EntityBeanComponentCreateService ejbComponentCreateService) {
        return new ReferenceCountingEntityCache(this);
    }

    public EJBLocalObject getEjbLocalObject(final Object primaryKey) {
        final HashMap<Object, Object> create = new HashMap<Object, Object>();
        create.put(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY, primaryKey);
        return createViewInstanceProxy(getLocalClass(), create);
    }

    public EJBObject getEJBObject(final Object primaryKey) {
        final HashMap<Object, Object> create = new HashMap<Object, Object>();
        create.put(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY, primaryKey);
        return createViewInstanceProxy(getRemoteClass(), create);
    }

    public Class<EJBHome> getHomeClass() {
        return homeClass;
    }

    public Class<EJBLocalHome> getLocalHomeClass() {
        return localHomeClass;
    }

    public Class<EJBLocalObject> getLocalClass() {
        return localClass;
    }

    public Class<EJBObject> getRemoteClass() {
        return remoteClass;
    }

}
