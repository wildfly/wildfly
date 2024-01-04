/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.txn.deployment;

import jakarta.transaction.TransactionManager;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.msc.service.ServiceName;

/**
 * Processor that adds a {@link SetupAction} to the deployment that prevents
 * transactions from leaking from web requests.
 *
 * @author Stuart Douglas
 */
public class TransactionLeakRollbackProcessor implements DeploymentUnitProcessor {

    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("transaction", "ee-transaction-rollback-service");
    private static final AttachmentKey<SetupAction> ATTACHMENT_KEY = AttachmentKey.create(SetupAction.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ServiceName serviceName = deploymentUnit.getServiceName().append(SERVICE_NAME);
        final TransactionRollbackSetupAction service = new TransactionRollbackSetupAction(serviceName);
        phaseContext.getServiceTarget().addService(serviceName, service)
                .addDependency(TransactionManagerService.INTERNAL_SERVICE_NAME, TransactionManager.class, service.getTransactionManager())
                .install();

        deploymentUnit.addToAttachmentList(Attachments.WEB_SETUP_ACTIONS, service);
        deploymentUnit.putAttachment(ATTACHMENT_KEY, service);
    }


    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        SetupAction action = deploymentUnit.removeAttachment(ATTACHMENT_KEY);
        deploymentUnit.getAttachmentList(Attachments.WEB_SETUP_ACTIONS).remove(action);
    }
}
