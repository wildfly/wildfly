/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.NoopOperationStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class LogStoreDefinition extends SimpleResourceDefinition {

    private final boolean registerRuntimeOnly;

    public LogStoreDefinition(final LogStoreResource resource, final boolean registerRuntimeOnly) {
        super(new SimpleResourceDefinition.Parameters(TransactionExtension.LOG_STORE_PATH, TransactionExtension.getResourceDescriptionResolver(LogStoreConstants.LOG_STORE))
                .setAddHandler(new LogStoreAddHandler(resource))
                .setRemoveHandler(NoopOperationStepHandler.WITH_RESULT)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_NONE));
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        final OperationDefinition probe = new SimpleOperationDefinitionBuilder(LogStoreConstants.PROBE, getResourceDescriptionResolver())
                                .withFlag(OperationEntry.Flag.HOST_CONTROLLER_ONLY) // TODO WFLY-8852 decide how we want to handle this in a domain
                                .setRuntimeOnly()
                                .setReadOnly()
                                .build();
        resourceRegistration.registerOperationHandler(probe, LogStoreProbeHandler.INSTANCE);
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadOnlyAttribute(LogStoreConstants.LOG_STORE_TYPE, null);
        if (registerRuntimeOnly) {
            resourceRegistration.registerReadWriteAttribute(LogStoreConstants.EXPOSE_ALL_LOGS, null,
                    new ExposeAllLogsWriteAttributeHandler());
        }
    }

    static class ExposeAllLogsWriteAttributeHandler extends AbstractRuntimeOnlyHandler {
        @Override
        protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
            final String attributeName = operation.require(NAME).asString();
            ModelNode newValue = operation.hasDefined(VALUE)
                    ? operation.get(VALUE) : LogStoreConstants.EXPOSE_ALL_LOGS.getDefaultValue();

            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode submodel = resource.getModel();
            final ModelNode syntheticOp = new ModelNode();
            syntheticOp.get(attributeName).set(newValue);
            LogStoreConstants.EXPOSE_ALL_LOGS.validateAndSet(syntheticOp, submodel);

            // ExposeAllRecordsAsMBeans JMX attribute will be set in LogStoreProbeHandler prior to eventual probe operation execution,
            // hence no need to do here anything else

            context.getResult().set(new ModelNode());
            context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
        }
    }

}
