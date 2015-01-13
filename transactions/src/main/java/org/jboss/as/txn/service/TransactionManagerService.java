/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn.service;

import javax.transaction.TransactionManager;

import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for getting the {@link TransactionManager}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 29-Oct-2010
 */
public class TransactionManagerService extends AbstractService<TransactionManager> {

    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_TRANSACTION_MANAGER;

    private final InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService> injectedArjunaTM = new InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService>();

    public static ServiceController<TransactionManager> addService(final ServiceTarget target) {
        TransactionManagerService service = new TransactionManagerService();
        ServiceBuilder<TransactionManager> serviceBuilder = target.addService(SERVICE_NAME, service);
        serviceBuilder.addDependency(ArjunaTransactionManagerService.SERVICE_NAME, com.arjuna.ats.jbossatx.jta.TransactionManagerService.class, service.injectedArjunaTM);
        return serviceBuilder.install();
    }

    @Override
    public TransactionManager getValue() throws IllegalStateException {
        return injectedArjunaTM.getValue().getTransactionManager();
    }
}
