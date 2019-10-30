package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.JcaCachedConnectionManagerDefinition.CcmParameters.DEBUG;
import static org.jboss.as.connector.subsystems.jca.JcaCachedConnectionManagerDefinition.CcmParameters.ERROR;
import static org.jboss.as.connector.subsystems.jca.JcaCachedConnectionManagerDefinition.CcmParameters.IGNORE_UNKNOWN_CONNECTIONS;
import static org.jboss.as.connector.subsystems.jca.JcaCachedConnectionManagerDefinition.CcmParameters.INSTALL;

import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;

class CachedConnectionManagerRemove implements OperationStepHandler {

    static final CachedConnectionManagerRemove INSTANCE = new CachedConnectionManagerRemove();

    private CachedConnectionManagerRemove() {}

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // This is an odd case where we do not actually do a remove; we just reset state to
        // what it would be following parsing if the xml element does not exist.
        // See discussion on PR with fix for WFLY-2640 .
        ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();

        for (JcaCachedConnectionManagerDefinition.CcmParameters param : JcaCachedConnectionManagerDefinition.CcmParameters.values()) {
            AttributeDefinition ad = param.getAttribute();
            if (param == INSTALL || param == DEBUG || param == ERROR || param == IGNORE_UNKNOWN_CONNECTIONS) {
                model.get(ad.getName()).clear();
            } else {
                // Someone added a new param since wFLY-2640/WFLY-8141 and did not account for it above
                throw new IllegalStateException();
            }
        }

        // At the time of WFLY-2640 there were no capabilities associated with this resource,
        // but if anyone adds one, part of the task is to deal with deregistration.
        // So here's an assert to ensure that is considered
        Set<RuntimeCapability> capabilitySet = context.getResourceRegistration().getCapabilities();
        assert capabilitySet.isEmpty();

        if (context.isDefaultRequiresRuntime()) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext operationContext, ModelNode modelNode) throws OperationFailedException {
                    context.reloadRequired();
                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext operationContext, ModelNode modelNode) {
                            context.revertReloadRequired();
                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }
}
