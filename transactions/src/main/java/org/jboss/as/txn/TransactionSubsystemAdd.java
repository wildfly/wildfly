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

package org.jboss.as.txn;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.services.net.SocketBinding;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.tm.JBossXATerminator;
import org.omg.CORBA.ORB;

import javax.transaction.TransactionManager;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class TransactionSubsystemAdd extends AbstractSubsystemAdd<TransactionsSubsystemElement> {

    private static final long serialVersionUID = 6904905763469774913L;

    private String recoveryBindingName;
    private String recoveryStatusBindingName;
    private String nodeIdentifier;
    private String bindingName;
    private boolean coordinatorEnableStatistics;
    private String objectStoreDirectory;
    private int maxPorts = 10;
    private int coordinatorDefaultTimeout = 300;

    protected TransactionSubsystemAdd() {
        super(Namespace.CURRENT.getUriString());
    }

    @Override
    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
        final BatchBuilder builder = updateContext.getBatchBuilder();
        // XATerminator has no deps, so just add it in there
        final XATerminatorService xaTerminatorService = new XATerminatorService();
        builder.addService(TxnServices.JBOSS_TXN_XA_TERMINATOR, xaTerminatorService).setInitialMode(ServiceController.Mode.IMMEDIATE);

        final TransactionManagerService transactionManagerService = new TransactionManagerService(nodeIdentifier, maxPorts, coordinatorEnableStatistics, coordinatorDefaultTimeout, objectStoreDirectory);
        final BatchServiceBuilder<TransactionManager> transactionManagerServiceBuilder = builder.addService(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, transactionManagerService);
        transactionManagerServiceBuilder.addOptionalDependency(ServiceName.JBOSS.append("iiop", "orb"), ORB.class, transactionManagerService.getOrbInjector());
        transactionManagerServiceBuilder.addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, transactionManagerService.getXaTerminatorInjector());
        transactionManagerServiceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(recoveryBindingName), SocketBinding.class, transactionManagerService.getRecoveryBindingInjector());
        transactionManagerServiceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(recoveryStatusBindingName), SocketBinding.class, transactionManagerService.getStatusBindingInjector());
        transactionManagerServiceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingName), SocketBinding.class, transactionManagerService.getSocketProcessBindingInjector());
        transactionManagerServiceBuilder.setInitialMode(ServiceController.Mode.IMMEDIATE);
    }

    @Override
    protected TransactionsSubsystemElement createSubsystemElement() {
        TransactionsSubsystemElement element = new TransactionsSubsystemElement();
        element.getCoreEnvironmentElement().setBindingRef(bindingName);
        element.getCoreEnvironmentElement().setMaxPorts(maxPorts);
        element.getCoreEnvironmentElement().setNodeIdentifier(nodeIdentifier);
        element.getRecoveryEnvironmentElement().setBindingRef(recoveryBindingName);
        element.getRecoveryEnvironmentElement().setStatusBindingRef(recoveryStatusBindingName);
        element.setObjectStoreDirectory(objectStoreDirectory);
        element.setCoordinatorDefaultTimeout(coordinatorDefaultTimeout);
        element.setCoordinatorEnableStatistics(coordinatorEnableStatistics);
        return element;
    }

    public String getRecoveryBindingName() {
        return recoveryBindingName;
    }

    public void setRecoveryBindingName(final String recoveryBindingName) {
        this.recoveryBindingName = recoveryBindingName;
    }

    public String getRecoveryStatusBindingName() {
        return recoveryStatusBindingName;
    }

    public void setRecoveryStatusBindingName(final String recoveryStatusBindingName) {
        this.recoveryStatusBindingName = recoveryStatusBindingName;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public void setNodeIdentifier(final String nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
    }

    public String getBindingName() {
        return bindingName;
    }

    public void setBindingName(final String bindingName) {
        this.bindingName = bindingName;
    }

    public boolean isCoordinatorEnableStatistics() {
        return coordinatorEnableStatistics;
    }

    public void setCoordinatorEnableStatistics(final boolean coordinatorEnableStatistics) {
        this.coordinatorEnableStatistics = coordinatorEnableStatistics;
    }

    public void setCoordinatorDefaultTimeout(final int timeout) {
        this.coordinatorDefaultTimeout = timeout;
    }

    public String getObjectStoreDirectory() {
        return objectStoreDirectory;
    }

    public void setObjectStoreDirectory(final String objectStoreDirectory) {
        this.objectStoreDirectory = objectStoreDirectory;
    }

    public int getMaxPorts() {
        return maxPorts;
    }

    public void setMaxPorts(final int maxPorts) {
        this.maxPorts = maxPorts;
    }
}
