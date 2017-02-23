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

import org.jboss.as.connector.deployers.ra.processors.CachedConnectionManagerSetupProcessor;
import org.jboss.as.connector.services.jca.CachedConnectionManagerService;
import org.jboss.as.connector.services.jca.NonTxCachedConnectionManagerService;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.msc.service.ServiceTarget;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;
import static org.jboss.as.connector.subsystems.jca.JcaCachedConnectionManagerDefinition.CcmParameters.DEBUG;
import static org.jboss.as.connector.subsystems.jca.JcaCachedConnectionManagerDefinition.CcmParameters.ERROR;
import static org.jboss.as.connector.subsystems.jca.JcaCachedConnectionManagerDefinition.CcmParameters.IGNORE_UNKNOWN_CONNECTIONS;
import static org.jboss.as.connector.subsystems.jca.JcaCachedConnectionManagerDefinition.CcmParameters.INSTALL;

import java.util.Set;

/**
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class CachedConnectionManagerAdd extends AbstractBoottimeAddStepHandler {

    public static final CachedConnectionManagerAdd INSTANCE = new CachedConnectionManagerAdd();

    @Override
    protected Resource createResource(OperationContext context, ModelNode operation) {
        // WFLY-2640/WFLY-8141 This resource is an odd case because the *parser* will insert a
        // resource add op even if the config says nothing, but we also have to support
        // cases where subsystem isn't added and this resource is added via a management op.
        // To cover all the cases we need to allow add to succeed when the resource already
        // exists, so long as it is configured with the values the parser generates (i.e. no defined attributes)
        try {
            Resource existing = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS); // will fail in normal case
            ModelNode model = existing.getModel();
            boolean allDefault = true;
            for (JcaCachedConnectionManagerDefinition.CcmParameters param : JcaCachedConnectionManagerDefinition.CcmParameters.values()) {
                if (param == INSTALL || param == DEBUG || param == ERROR || param == IGNORE_UNKNOWN_CONNECTIONS) {
                    AttributeDefinition ad = param.getAttribute();
                    assert !ad.isRequired() : ad.getName(); // else someone changed something and we need to account for that
                    if (model.hasDefined(ad.getName())) {
                        allDefault = false;
                        break;
                    }
                } else {
                    // Someone added a new param since WFLY-2640/WFLY-8141 and did not account for it above
                    throw new IllegalStateException();
                }
            }
            if (allDefault) {
                // We can use the existing resource as if we just created it
                return existing;
            } // else fall through and call super method which will fail due to resource already being present

        } catch (Resource.NoSuchResourceException normal) {
            // normal case; resource doesn't exist yet so fall through
        }

        return super.createResource(context, operation);
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (JcaCachedConnectionManagerDefinition.CcmParameters parameter : JcaCachedConnectionManagerDefinition.CcmParameters.values()) {
            parameter.getAttribute().validateAndSet(operation, model);
        }
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        // At the time of WFLY-2640/WFLY-8141 there were no capabilities associated with this resource,
        // but if anyone adds one, part of the task is to deal with possible reregistration.
        // So here's an assert to ensure that is considered
        Set<RuntimeCapability> capabilitySet = context.getResourceRegistration().getCapabilities();
        assert capabilitySet.isEmpty();
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {

        final boolean debug = JcaCachedConnectionManagerDefinition.CcmParameters.DEBUG.getAttribute().resolveModelAttribute(context, model).asBoolean();
        final boolean error = JcaCachedConnectionManagerDefinition.CcmParameters.ERROR.getAttribute().resolveModelAttribute(context, model).asBoolean();
        final boolean ignoreUnknownConnections = JcaCachedConnectionManagerDefinition.CcmParameters.IGNORE_UNKNOWN_CONNECTIONS.getAttribute().resolveModelAttribute(context, model).asBoolean();

        final boolean install = JcaCachedConnectionManagerDefinition.CcmParameters.INSTALL.getAttribute().resolveModelAttribute(context, model).asBoolean();

        final ServiceTarget serviceTarget = context.getServiceTarget();

        if (install) {
            ROOT_LOGGER.debug("Enabling the Cache Connection Manager valve and interceptor...");
            context.addStep(new AbstractDeploymentChainStep() {
                protected void execute(DeploymentProcessorTarget processorTarget) {
                    processorTarget.addDeploymentProcessor(JcaExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_CACHED_CONNECTION_MANAGER, new CachedConnectionManagerSetupProcessor());
                }
            }, OperationContext.Stage.RUNTIME);
        } else {
            ROOT_LOGGER.debug("Disabling the Cache Connection Manager valve and interceptor...");
        }

        CachedConnectionManagerService ccmService = new CachedConnectionManagerService(debug, error, ignoreUnknownConnections);
        serviceTarget
                .addService(ConnectorServices.CCM_SERVICE, ccmService)
                .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class,
                        ccmService.getTransactionIntegrationInjector())
                .install();

        NonTxCachedConnectionManagerService noTxCcm = new NonTxCachedConnectionManagerService(debug, error, ignoreUnknownConnections);
        serviceTarget
            .addService(ConnectorServices.NON_TX_CCM_SERVICE, noTxCcm)
            .install();

    }
}
