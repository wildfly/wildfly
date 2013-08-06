package org.wildfly.clustering.monitor.extension;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Handler responsible for removing the subsystem resource from the model
 *
 * @author Richard Achmatowicz (c) Red Hat 2013
 */
class ClusterSubsystemRemove extends AbstractRemoveStepHandler {

    static final ClusterSubsystemRemove INSTANCE = new ClusterSubsystemRemove();

    private ClusterSubsystemRemove() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.removeService(ClusterExtension.CLUSTER_EXTENSION_SERVICE_NAME);
    }
}
