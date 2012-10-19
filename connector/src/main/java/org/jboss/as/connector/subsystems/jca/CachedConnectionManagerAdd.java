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
package org.jboss.as.connector.subsystems.jca;

import java.util.List;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.connector.deployers.ra.processors.CachedConnectionManagerSetupProcessor;
import org.jboss.as.connector.services.jca.CachedConnectionManagerService;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class CachedConnectionManagerAdd extends AbstractAddStepHandler {

    public static final CachedConnectionManagerAdd INSTANCE = new CachedConnectionManagerAdd();

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (JcaCachedConnectionManagerDefinition.CcmParameters parameter : JcaCachedConnectionManagerDefinition.CcmParameters.values()) {
            parameter.getAttribute().validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                   final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final boolean debug = JcaCachedConnectionManagerDefinition.CcmParameters.DEBUG.getAttribute().resolveModelAttribute(context, model).asBoolean();
        final boolean error = JcaCachedConnectionManagerDefinition.CcmParameters.ERROR.getAttribute().resolveModelAttribute(context, model).asBoolean();
        final boolean install = JcaCachedConnectionManagerDefinition.CcmParameters.INSTALL.getAttribute().resolveModelAttribute(context, model).asBoolean();

        final ServiceTarget serviceTarget = context.getServiceTarget();

        if (install) {
            context.addStep(new AbstractDeploymentChainStep() {
                protected void execute(DeploymentProcessorTarget processorTarget) {
                    processorTarget.addDeploymentProcessor(JcaExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_CACHED_CONNECTION_MANAGER, new CachedConnectionManagerSetupProcessor());
                }
            }, OperationContext.Stage.RUNTIME);
        }

        CachedConnectionManagerService ccmService = new CachedConnectionManagerService(debug, error);
        newControllers.add(serviceTarget
                .addService(ConnectorServices.CCM_SERVICE, ccmService)
                .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class,
                        ccmService.getTransactionIntegrationInjector())
                .addListener(verificationHandler)
                .install());

    }
}
