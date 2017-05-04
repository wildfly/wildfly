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
package org.jboss.as.txn.deployment;

import javax.transaction.TransactionManager;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.msc.service.ServiceName;

/**
 * Processor that adds a {@link org.jboss.as.server.deployment.SetupAction} to the deployment that prevents
 * transactions from leaking from web requests.
 *
 * @author Stuart Douglas
 */
public class TransactionLeakRollbackProcessor implements DeploymentUnitProcessor {

    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("transaction", "ee-transaction-rollback-service");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ServiceName serviceName = deploymentUnit.getServiceName().append(SERVICE_NAME);
        final TransactionRollbackSetupAction service = new TransactionRollbackSetupAction(serviceName);
        phaseContext.getServiceTarget().addService(serviceName, service)
                .addDependency(TransactionManagerService.SERVICE_NAME, TransactionManager.class, service.getTransactionManager())
                .install();

        deploymentUnit.addToAttachmentList(Attachments.WEB_SETUP_ACTIONS, service);
    }


    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        deploymentUnit.getAttachmentList(Attachments.WEB_SETUP_ACTIONS).removeIf(setupAction -> setupAction instanceof TransactionRollbackSetupAction);
    }
}
