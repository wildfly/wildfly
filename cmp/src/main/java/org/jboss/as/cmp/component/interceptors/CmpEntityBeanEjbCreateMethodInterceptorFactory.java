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

package org.jboss.as.cmp.component.interceptors;

import java.lang.reflect.Method;
import javax.transaction.Transaction;
import org.jboss.as.cmp.TransactionEntityMap;
import org.jboss.as.cmp.component.CmpEntityBeanComponent;
import org.jboss.as.cmp.component.CmpEntityBeanComponentInstance;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanEjbCreateMethodInterceptorFactory;
import org.jboss.invocation.InterceptorContext;
import org.jboss.tm.TxUtils;

/**
 * @author John Bailey
 */
public class CmpEntityBeanEjbCreateMethodInterceptorFactory extends EntityBeanEjbCreateMethodInterceptorFactory {

    public static final CmpEntityBeanEjbCreateMethodInterceptorFactory INSTANCE = new CmpEntityBeanEjbCreateMethodInterceptorFactory();

    protected Object invokeEjbCreate(final InterceptorContext context, final Method ejbCreate, final EntityBeanComponentInstance instance, final Object[] params) throws Exception {
        final CmpEntityBeanComponentInstance cmpInstance = CmpEntityBeanComponentInstance.class.cast(instance);
        final JDBCEntityPersistenceStore storeManager = cmpInstance.getComponent().getStoreManager();
        storeManager.initEntity(cmpInstance.getEjbContext());
        ejbCreate.invoke(instance.getInstance(), params);
        return storeManager.createEntity(context.getMethod(), context.getParameters(), cmpInstance.getEjbContext());
    }

    protected void invokeEjbPostCreate(final InterceptorContext context, final Method ejbPostCreate, final EntityBeanComponentInstance instance, final Object[] params) throws Exception {
        final CmpEntityBeanComponentInstance cmpInstance = CmpEntityBeanComponentInstance.class.cast(instance);
        final CmpEntityBeanComponent component = cmpInstance.getComponent();
        final JDBCEntityPersistenceStore storeManager = component.getStoreManager();
        storeManager.postCreateEntity(context.getMethod(), context.getParameters(), cmpInstance.getEjbContext());
        ejbPostCreate.invoke(instance.getInstance(), params);

        if (storeManager.getCmpConfig().isInsertAfterEjbPostCreate()) {
            storeManager.createEntity(context.getMethod(), context.getParameters(), cmpInstance.getEjbContext());
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
