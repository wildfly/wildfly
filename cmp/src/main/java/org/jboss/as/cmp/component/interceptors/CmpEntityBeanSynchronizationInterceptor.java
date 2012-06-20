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

import javax.transaction.Transaction;

import org.jboss.as.cmp.component.CmpEntityBeanComponent;
import org.jboss.as.cmp.component.CmpEntityBeanComponentInstance;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ComponentInstanceInterceptorFactory;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanSynchronizationInterceptor;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.tm.TxUtils;

/**
 * @author John Bailey
 */
public class CmpEntityBeanSynchronizationInterceptor extends EntityBeanSynchronizationInterceptor {
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final CmpEntityBeanComponent component = (CmpEntityBeanComponent) context.getPrivateData(Component.class);
        final CmpEntityBeanComponentInstance instance = (CmpEntityBeanComponentInstance) context.getPrivateData(ComponentInstance.class);
        //we do not synchronize for instances that are not associated with an identity
        if (instance.getPrimaryKey() == null) {
            return context.proceed();
        }
        final CmpEntityBeanContext entityContext = instance.getEjbContext();

        if (instance.isReloadRequired()) {
            synchronized (instance) {
                if (instance.isReloadRequired()) {
                    instance.reload();
                }
            }
        }

        // now it's ready and can be scheduled for the synchronization
        final Transaction transaction = component.getTransactionManager().getTransaction();
        if (TxUtils.isActive(transaction)) {
            entityContext.getTxAssociation().scheduleSync(transaction, instance.getEjbContext());
        }

        return super.processInvocation(context);
    }

    public static final InterceptorFactory FACTORY = new ComponentInstanceInterceptorFactory() {
        @Override
        protected Interceptor create(final Component component, final InterceptorFactoryContext context) {
            return new CmpEntityBeanSynchronizationInterceptor();
        }
    };
}
