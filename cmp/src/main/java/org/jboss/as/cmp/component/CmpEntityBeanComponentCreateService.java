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

import org.jboss.as.cmp.TransactionEntityMap;
import org.jboss.as.cmp.component.interceptors.CmpEntityBeanJdbcRelationshipInterceptor;
import org.jboss.as.cmp.component.interceptors.CmpEntityBeanSynchronizationInterceptor;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.BasicComponentCreateService;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentCreateServiceFactory;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.component.interceptors.OrderedItemContainer;
import org.jboss.as.ejb3.component.EJBComponentCreateServiceFactory;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentCreateService;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanReentrancyInterceptor;
import org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

/**
 * @author John Bailey
 */
public class CmpEntityBeanComponentCreateService extends EntityBeanComponentCreateService {

    public static final ComponentCreateServiceFactory FACTORY = new EJBComponentCreateServiceFactory() {
        public BasicComponentCreateService constructService(final ComponentConfiguration configuration) {
            return new CmpEntityBeanComponentCreateService(configuration, this.ejbJarConfiguration);
        }
    };

    private final InjectedValue<TransactionEntityMap> transactionEntityMap = new InjectedValue<TransactionEntityMap>();

    private final JDBCEntityMetaData entityMetaData;
    private final InterceptorFactory relationInterceptorFactory;
    private Value<JDBCEntityPersistenceStore> storeManager;

    public CmpEntityBeanComponentCreateService(final ComponentConfiguration componentConfiguration, final ApplicationExceptions applicationExceptions) {
        super(componentConfiguration, applicationExceptions);
        final CmpEntityBeanComponentDescription cmpDescription = CmpEntityBeanComponentDescription.class.cast(componentConfiguration.getComponentDescription());
        entityMetaData = cmpDescription.getEntityMetaData();

        final OrderedItemContainer interceptors = new OrderedItemContainer();
        interceptors.add(CmpEntityBeanSynchronizationInterceptor.FACTORY, InterceptorOrder.Component.SYNCHRONIZATION_INTERCEPTOR);
        interceptors.add(CurrentInvocationContextInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.EJB_SESSION_CONTEXT_INTERCEPTOR);
        interceptors.add(CmpEntityBeanJdbcRelationshipInterceptor.FACTORY, InterceptorOrder.Component.CMP_RELATIONSHIP_INTERCEPTOR);

        this.relationInterceptorFactory = Interceptors.getChainedInterceptorFactory(interceptors.getSortedItems());
    }

    @Override
    protected BasicComponent createComponent() {
        return new CmpEntityBeanComponent(this, storeManager);
    }

    public JDBCEntityMetaData getEntityMetaData() {
        return entityMetaData;
    }

    public JDBCEntityPersistenceStore getStoreManager() {
        return storeManager.getValue();
    }

    public void setStoreManagerValue(final Value<JDBCEntityPersistenceStore> storeManager) {
        this.storeManager = storeManager;
    }

    public InterceptorFactory getRelationInterceptorFactory() {
        return relationInterceptorFactory;
    }

    public TransactionEntityMap getTransactionEntityMap() {
        return transactionEntityMap.getValue();
    }

    public Injector<TransactionEntityMap> getTransactionEntityMapInjector() {
        return transactionEntityMap;
    }
}
