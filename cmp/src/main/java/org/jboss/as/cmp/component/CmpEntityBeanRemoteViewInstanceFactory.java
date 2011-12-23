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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.transaction.Transaction;

import org.jboss.as.cmp.TransactionEntityMap;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.as.ejb3.component.entity.EntityBeanRemoteViewInstanceFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.tm.TxUtils;

/**
 * @author John Bailey
 */
public class CmpEntityBeanRemoteViewInstanceFactory extends EntityBeanRemoteViewInstanceFactory {
    public CmpEntityBeanRemoteViewInstanceFactory(String applicationName, String moduleName, String distinctName, String beanName) {
        super(applicationName, moduleName, distinctName, beanName);
    }

    protected Object invokeEjbCreate(final Map<Object, Object> contextData, final Method ejbCreate, final EntityBeanComponentInstance instance, final Object[] params) throws Exception {
        final CmpEntityBeanComponentInstance cmpInstance = CmpEntityBeanComponentInstance.class.cast(instance);
        final JDBCEntityPersistenceStore storeManager = cmpInstance.getComponent().getStoreManager();
        storeManager.initEntity(cmpInstance.getEjbContext());
        super.invokeEjbCreate(contextData, ejbCreate, instance, params);
        return storeManager.createEntity(ejbCreate, params, cmpInstance.getEjbContext());
    }

    protected void invokeEjbPostCreate(final Map<Object, Object> contextData, final Method ejbPostCreate, final EntityBeanComponentInstance instance, final Object[] params) throws Exception {
        final CmpEntityBeanComponentInstance cmpInstance = CmpEntityBeanComponentInstance.class.cast(instance);
        final CmpEntityBeanComponent component = cmpInstance.getComponent();
        final JDBCEntityPersistenceStore storeManager = component.getStoreManager();
        storeManager.postCreateEntity(ejbPostCreate, params, cmpInstance.getEjbContext());
        try {
            ejbPostCreate.invoke(instance.getInstance(), params);
        } catch (InvocationTargetException ite) {
            throw Interceptors.rethrow(ite.getCause());
        }

        if (storeManager.getCmpConfig().isInsertAfterEjbPostCreate()) {
            storeManager.createEntity(ejbPostCreate, params, cmpInstance.getEjbContext());
        } else {
            // Invoke store after post create
            cmpInstance.store();
        }

        final Transaction transaction = component.getTransactionManager().getTransaction();
        if (TxUtils.isActive(transaction)) {
            TransactionEntityMap.NONE.scheduleSync(transaction, cmpInstance.getEjbContext());
        }
    }
}
