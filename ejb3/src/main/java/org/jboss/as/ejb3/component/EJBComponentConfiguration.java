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
package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.deployment.EjbJarConfiguration;
import org.jboss.as.ee.component.EEModuleClassConfiguration;
import org.jboss.msc.service.ServiceBuilder;

import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class EJBComponentConfiguration extends ComponentConfiguration {
    private final TransactionManagementType transactionManagementType;
    private final ConcurrentMap<MethodIntf, ConcurrentMap<String, ConcurrentMap<ArrayKey, TransactionAttributeType>>> txAttrs;

    protected EjbJarConfiguration ejbJarConfiguration;

    /**
     * Construct a new instance.
     *
     * @param description the original component description
     */
    public EJBComponentConfiguration(final EJBComponentDescription description, final EEModuleClassConfiguration ejbModuleClassConfiguration) {
        super(description, ejbModuleClassConfiguration);

        description.addDependency(EJBUtilities.SERVICE_NAME, ServiceBuilder.DependencyType.REQUIRED);

        // CurrentInvocationContext
        addCurrentInvocationContextInterceptorFactory();

        transactionManagementType = description.getTransactionManagementType();

        // CMTTx
//        if (transactionManagementType.equals(TransactionManagementType.CONTAINER)) {
//            // slurp some memory
//            txAttrs = new ConcurrentHashMap<MethodIntf, ConcurrentMap<String, ConcurrentMap<ArrayKey, TransactionAttributeType>>>();
//            //TODO: interceptors
//            addComponentSystemInterceptorFactory(new ComponentInterceptorFactory() {
//                @Override
//                protected Interceptor create(Component component, InterceptorFactoryContext context) {
//                    return new CMTTxInterceptor((TransactionalComponent) component);
//                }
//            });
//        }

        txAttrs = null;

    }

    protected abstract void addCurrentInvocationContextInterceptorFactory();

    /**
     * @return the ejb-name
     */
    public String getName() {
        return getComponentName();
    }

    TransactionManagementType getTransactionManagementType() {
        return transactionManagementType;
    }

    ConcurrentMap<MethodIntf, ConcurrentMap<String, ConcurrentMap<ArrayKey, TransactionAttributeType>>> getTxAttrs() {
        return txAttrs;
    }

    void setEjbJarConfiguration(EjbJarConfiguration ejbJarConfiguration) {
        this.ejbJarConfiguration = ejbJarConfiguration;
    }

    public EjbJarConfiguration getEjbJarConfiguration() {
        return this.ejbJarConfiguration;
    }
}
