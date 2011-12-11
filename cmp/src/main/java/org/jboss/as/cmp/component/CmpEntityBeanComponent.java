/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cmp.component;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.ejb.EJBException;
import javax.ejb.EJBLocalHome;
import javax.transaction.Transaction;

import org.jboss.as.cmp.TransactionEntityMap;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.bridge.CMRMessage;
import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentCreateService;
import org.jboss.as.ejb3.component.entity.entitycache.ReadyEntityCache;
import org.jboss.as.ejb3.component.entity.entitycache.TransactionLocalEntityCache;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.SimpleInterceptorFactoryContext;
import org.jboss.msc.value.Value;

/**
 * @author John Bailey
 */
public class CmpEntityBeanComponent extends EntityBeanComponent {

    private final Value<JDBCEntityPersistenceStore> storeManager;
    private final InterceptorFactory relationInterceptorFactory;
    private boolean ejbStoreForClean;

    private final TransactionEntityMap transactionEntityMap;

    public CmpEntityBeanComponent(final CmpEntityBeanComponentCreateService ejbComponentCreateService, final Value<JDBCEntityPersistenceStore> storeManager) {
        super(ejbComponentCreateService);

        this.storeManager = storeManager;

        this.relationInterceptorFactory = ejbComponentCreateService.getRelationInterceptorFactory();
        this.transactionEntityMap = ejbComponentCreateService.getTransactionEntityMap();
    }

    protected BasicComponentInstance instantiateComponentInstance(final AtomicReference<ManagedReference> instanceReference, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors, final InterceptorFactoryContext interceptorContext) {
        final SimpleInterceptorFactoryContext factoryContext = new SimpleInterceptorFactoryContext();
        factoryContext.getContextData().put(Component.class, this);
        final Interceptor interceptor = relationInterceptorFactory.create(factoryContext);

        return new CmpEntityBeanComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors, interceptor);
    }

    public void start() {
        super.start();
        if (storeManager == null || storeManager.getValue() == null) {
            throw new IllegalStateException("Store manager not set");
        }
    }

    public Collection<Object> getEntityLocalCollection(List<Object> idList) {
        return null;  // TODO: jeb - This should return proxy instances to local entities
    }

    public void synchronizeEntitiesWithinTransaction(Transaction transaction) {
        // If there is no transaction, there is nothing to synchronize.
        if (transaction != null) {
            getTransactionEntityMap().synchronizeEntities(transaction);
        }
    }

    public EJBLocalHome getEJBLocalHome() throws IllegalStateException {
        return createViewInstanceProxy(getLocalHomeClass(), Collections.emptyMap());
    }

    public JDBCEntityPersistenceStore getStoreManager() {
        return storeManager.getValue();
    }


    public Object invoke(final CMRMessage message, final Object key, final Object... params) throws Exception {
        final CmpEntityBeanComponentInstance instance = (CmpEntityBeanComponentInstance) getCache().get(key);
        return instance.invoke(message, params);
    }

    /**
     * Invokes ejbStore method on the instance
     *
     * @param ctx the instance to invoke ejbStore on
     * @throws Exception
     */
    public void invokeEjbStore(CmpEntityBeanContext ctx) throws Exception {
        if (ctx.getPrimaryKey() != null) {
            // if call-ejb-store-for-clean=true then invoke ejbStore first (the last chance to modify the instance)
            if (ejbStoreForClean) {
                try {
                    ctx.getInstance().ejbStore();
                } catch (Exception e) {
                    throwRemoteException(e);
                }
            } else {
                // else check whether the instance is dirty and invoke ejbStore only if it is really dirty
                boolean modified = false;
                try {
                    modified = getStoreManager().isStoreRequired(ctx);
                } catch (Exception e) {
                    throwRemoteException(e);
                }

                if (modified) {
                    try {
                        ctx.getInstance().ejbStore();
                    } catch (Exception e) {
                        throwRemoteException(e);
                    }
                }
            }
        }
    }

    public void storeEntity(CmpEntityBeanContext ctx) throws Exception {
        if (ctx.getPrimaryKey() != null) {
            if (getStoreManager().isStoreRequired(ctx)) {
                getStoreManager().storeEntity(ctx);
            }
        }
    }

    private void throwRemoteException(Exception e)
            throws RemoteException {
        if (e instanceof RemoteException) {
            // Rethrow exception
            throw (RemoteException) e;
        } else if (e instanceof EJBException) {
            // Rethrow exception
            throw (EJBException) e;
        } else {
            // Wrap runtime exceptions
            throw new EJBException((Exception) e);
        }
    }

    public TransactionEntityMap getTransactionEntityMap() {
        return transactionEntityMap;
    }

    protected ReadyEntityCache createEntityCache(EntityBeanComponentCreateService ejbComponentCreateService) {
        if (CmpEntityBeanComponentCreateService.class.cast(ejbComponentCreateService).getEntityMetaData().getOptimisticLocking() == null) {
            return super.createEntityCache(ejbComponentCreateService);
        } else {
            return new TransactionLocalEntityCache(this);
        }
    }
}
