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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.entity.entitycache.ReadyEntityCache;
import org.jboss.as.ejb3.component.entity.entitycache.ReferenceCountingEntityCache;
import org.jboss.as.ejb3.pool.InfinitePool;
import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.pool.StatelessObjectFactory;
import org.jboss.as.ejb3.timerservice.EntityTimedObjectInvokerImpl;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.SimpleInterceptorFactoryContext;

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
    private final Class<Object> primaryKeyClass;

    private final Method ejbStoreMethod;
    private final Method ejbLoadMethod;
    private final Method ejbActivateMethod;
    private final Method ejbPassivateMethod;
    private final Method unsetEntityContextMethod;
    private final InterceptorFactory ejbStore;
    private final InterceptorFactory ejbLoad;
    private final InterceptorFactory ejbActivate;
    private final InterceptorFactory ejbPassivate;
    private final InterceptorFactory unsetEntityContext;

    private final TimedObjectInvoker timedObjectInvoker;

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
        this.primaryKeyClass = ejbComponentCreateService.getPrimaryKeyClass();

        this.ejbActivate = ejbComponentCreateService.getEjbActivate();
        this.ejbActivateMethod = ejbComponentCreateService.getEjbActivateMethod();
        this.ejbLoad = ejbComponentCreateService.getEjbLoad();
        this.ejbLoadMethod = ejbComponentCreateService.getEjbLoadMethod();
        this.ejbStore = ejbComponentCreateService.getEjbStore();
        this.ejbStoreMethod = ejbComponentCreateService.getEjbStoreMethod();
        this.ejbPassivate = ejbComponentCreateService.getEjbPassivate();
        this.ejbPassivateMethod = ejbComponentCreateService.getEjbPassivateMethod();
        this.unsetEntityContext = ejbComponentCreateService.getUnsetEntityContext();
        this.unsetEntityContextMethod = ejbComponentCreateService.getUnsetEntityContextMethod();
        final String deploymentName;
        if (ejbComponentCreateService.getDistinctName() == null || ejbComponentCreateService.getDistinctName().length() == 0) {
            deploymentName = ejbComponentCreateService.getApplicationName() + "." + ejbComponentCreateService.getModuleName();
        } else {
            deploymentName = ejbComponentCreateService.getApplicationName() + "." + ejbComponentCreateService.getModuleName() + "." + ejbComponentCreateService.getDistinctName();
        }
        this.timedObjectInvoker = new EntityTimedObjectInvokerImpl(this, deploymentName);
    }

    @Override
    protected BasicComponentInstance instantiateComponentInstance(final AtomicReference<ManagedReference> instanceReference, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors, final InterceptorFactoryContext interceptorContext) {
        final Map<Method, Interceptor> timeouts;
        if (timeoutInterceptors != null) {
            timeouts = new HashMap<Method, Interceptor>();
            for (Map.Entry<Method, InterceptorFactory> entry : timeoutInterceptors.entrySet()) {
                timeouts.put(entry.getKey(), entry.getValue().create(interceptorContext));
            }
        } else {
            timeouts = Collections.emptyMap();
        }
        return new EntityBeanComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors, timeouts);
    }

    protected Interceptor createInterceptor(final InterceptorFactory factory) {
        if (factory == null)
            return null;
        final SimpleInterceptorFactoryContext context = new SimpleInterceptorFactoryContext();
        context.getContextData().put(Component.class, this);
        return factory.create(context);
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

    public Class<Object> getPrimaryKeyClass() {
        return primaryKeyClass;
    }

    public Method getEjbStoreMethod() {
        return ejbStoreMethod;
    }

    public Method getEjbLoadMethod() {
        return ejbLoadMethod;
    }

    public Method getEjbActivateMethod() {
        return ejbActivateMethod;
    }

    public InterceptorFactory getEjbStore() {
        return ejbStore;
    }

    public InterceptorFactory getEjbLoad() {
        return ejbLoad;
    }

    public InterceptorFactory getEjbActivate() {
        return ejbActivate;
    }

    public Method getEjbPassivateMethod() {
        return ejbPassivateMethod;
    }

    public InterceptorFactory getEjbPassivate() {
        return ejbPassivate;
    }

    public Method getUnsetEntityContextMethod() {
        return unsetEntityContextMethod;
    }

    public InterceptorFactory getUnsetEntityContext() {
        return unsetEntityContext;
    }

    @Override
    public TimedObjectInvoker getTimedObjectInvoker() {
        return timedObjectInvoker;
    }
}
