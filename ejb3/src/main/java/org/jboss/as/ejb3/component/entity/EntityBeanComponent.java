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
import java.util.Map;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.entity.entitycache.ReadyEntityCache;
import org.jboss.as.ejb3.component.entity.entitycache.ReferenceCountingEntityCache;
import org.jboss.as.ejb3.component.entity.entitycache.TransactionLocalEntityCache;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.pool.PooledComponent;
import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.pool.StatelessObjectFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

/**
 * @author Stuart Douglas
 */
public class EntityBeanComponent extends EJBComponent implements PooledComponent<EntityBeanComponentInstance> {

    public static final Object PRIMARY_KEY_CONTEXT_KEY = new Object();

    private final StatelessObjectFactory<EntityBeanComponentInstance> factory;
    private final Pool<EntityBeanComponentInstance> pool;
    private final String poolName;
    private final ReadyEntityCache cache;
    private final Class<EJBHome> homeClass;
    private final Class<EJBLocalHome> localHomeClass;
    private final Class<EJBLocalObject> localClass;
    private final Class<EJBObject> remoteClass;
    private final Class<?> primaryKeyClass;
    private final Boolean optimisticLocking;

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
    private Interceptor ejbStoreInterceptor;
    private Interceptor ejbLoadInterceptor;
    private Interceptor ejbActivateInterceptor;
    private Interceptor ejbPassivateInterceptor;
    private Interceptor unsetEntityContextInterceptor;

    protected EntityBeanComponent(final EntityBeanComponentCreateService ejbComponentCreateService) {
        super(ejbComponentCreateService);

        factory = new StatelessObjectFactory<EntityBeanComponentInstance>() {
            @Override
            public EntityBeanComponentInstance create() {
                return (EntityBeanComponentInstance) createInstance();
            }

            @Override
            public void destroy(EntityBeanComponentInstance obj) {
                obj.destroy();
            }
        };
        optimisticLocking = ejbComponentCreateService.getOptimisticLocking();
        final PoolConfig poolConfig = ejbComponentCreateService.getPoolConfig();
        if (poolConfig == null) {
            ROOT_LOGGER.debugf("Pooling is disabled for entity bean %s", ejbComponentCreateService.getComponentName());
            this.pool = null;
            this.poolName = null;
        } else {
            ROOT_LOGGER.debugf("Using pool config %s to create pool for entity bean %s", poolConfig, ejbComponentCreateService.getComponentName());
            this.pool = poolConfig.createPool(factory);
            this.poolName = poolConfig.getPoolName();
        }
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

    }

    @Override
    protected BasicComponentInstance instantiateComponentInstance(final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors, Map<Object, Object> context) {
        return new EntityBeanComponentInstance(this, preDestroyInterceptor, methodInterceptors);
    }

    public EntityBeanComponentInstance acquireUnAssociatedInstance() {
        if (pool != null) {
            return pool.get();
        } else {
            return factory.create();
        }
    }

    @Override
    protected void createInterceptors(InterceptorFactoryContext context) {
        super.createInterceptors(context);
        ejbStoreInterceptor = ejbStore.create(context);
        ejbLoadInterceptor = ejbLoad.create(context);
        ejbActivateInterceptor = ejbActivate.create(context);
        ejbPassivateInterceptor = ejbPassivate.create(context);
        unsetEntityContextInterceptor = unsetEntityContext.create(context);
    }

    public void releaseEntityBeanInstance(final EntityBeanComponentInstance instance) {
        if (pool != null) {
            pool.release(instance);
        } else {
            factory.destroy(instance);
        }
    }

    public ReadyEntityCache getCache() {
        return cache;
    }

    protected ReadyEntityCache createEntityCache(EntityBeanComponentCreateService ejbComponentCreateService) {
        if (optimisticLocking == null || !optimisticLocking) {
            return new ReferenceCountingEntityCache(this);
        } else {
            return new TransactionLocalEntityCache(this);
        }
    }


    public EJBLocalObject getEJBLocalObject(final Object pk) throws IllegalStateException {
        if (getEjbLocalObjectViewServiceName() == null) {
            throw EjbLogger.ROOT_LOGGER.beanComponentMissingEjbObject(getComponentName(), "EJBLocalObject");
        }
        return createViewInstanceProxy(EJBLocalObject.class, Collections.singletonMap(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY, pk), getEjbLocalObjectViewServiceName());
    }

    public EJBObject getEJBObject(final Object pk) throws IllegalStateException {
        if (getEjbObjectViewServiceName() == null) {
            throw EjbLogger.ROOT_LOGGER.beanComponentMissingEjbObject(getComponentName(), "EJBObject");
        }
        return createViewInstanceProxy(EJBObject.class, Collections.singletonMap(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY, pk), getEjbObjectViewServiceName());
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

    public Class<?> getPrimaryKeyClass() {
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

    public Interceptor getEjbStore() {
        return ejbStoreInterceptor;
    }

    public Interceptor getEjbLoad() {
        return ejbLoadInterceptor;
    }

    public Interceptor getEjbActivate() {
        return ejbActivateInterceptor;
    }

    public Method getEjbPassivateMethod() {
        return ejbPassivateMethod;
    }

    public Interceptor getEjbPassivate() {
        return ejbPassivateInterceptor;
    }

    public Method getUnsetEntityContextMethod() {
        return unsetEntityContextMethod;
    }

    public Interceptor getUnsetEntityContext() {
        return unsetEntityContextInterceptor;
    }

    public Pool<EntityBeanComponentInstance> getPool() {
        return pool;
    }

    @Override
    public String getPoolName() {
        return poolName;
    }

    @Override
    public AllowedMethodsInformation getAllowedMethodsInformation() {
        return EntityBeanAllowedMethodsInformation.INSTANCE;
    }
}
