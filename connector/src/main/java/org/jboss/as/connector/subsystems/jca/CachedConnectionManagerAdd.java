/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
                .addDependency(context.getCapabilityServiceSupport().getCapabilityServiceName(ConnectorServices.TRANSACTION_INTEGRATION_CAPABILITY_NAME), TransactionIntegration.class,
                        ccmService.getTransactionIntegrationInjector())
                .install();

        NonTxCachedConnectionManagerService noTxCcm = new NonTxCachedConnectionManagerService(debug, error, ignoreUnknownConnections);
        serviceTarget
            .addService(ConnectorServices.NON_TX_CCM_SERVICE, noTxCcm)
            .install();

    }
}
