/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.txn.deployment;

import jakarta.transaction.TransactionManager;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.RequirementServiceBuilder;
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
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class TransactionLeakRollbackProcessor implements DeploymentUnitProcessor {

    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("transaction", "ee-transaction-rollback-service");
    private static final AttachmentKey<SetupAction> ATTACHMENT_KEY = AttachmentKey.create(SetupAction.class);

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ServiceName serviceName = deploymentUnit.getServiceName().append(SERVICE_NAME);
        final RequirementServiceBuilder<?> rsb = phaseContext.getRequirementServiceTarget().addService();
        final Consumer<TransactionRollbackSetupAction> txnRollbackSetupActionConsumer = rsb.provides(serviceName);
        final Supplier<TransactionManager> tmSupplier = rsb.requires(TransactionManagerService.INTERNAL_SERVICE_NAME);
        final TransactionRollbackSetupAction service = new TransactionRollbackSetupAction(txnRollbackSetupActionConsumer, tmSupplier, serviceName);
        rsb.setInstance(service);
        rsb.install();

        deploymentUnit.addToAttachmentList(Attachments.WEB_SETUP_ACTIONS, service);
        deploymentUnit.putAttachment(ATTACHMENT_KEY, service);
    }

    @Override
    public void undeploy(final DeploymentUnit deploymentUnit) {
        final SetupAction action = deploymentUnit.removeAttachment(ATTACHMENT_KEY);
        deploymentUnit.getAttachmentList(Attachments.WEB_SETUP_ACTIONS).remove(action);
    }
}
