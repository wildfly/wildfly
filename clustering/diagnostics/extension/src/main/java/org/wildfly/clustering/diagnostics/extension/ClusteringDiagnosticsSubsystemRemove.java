package org.wildfly.clustering.diagnostics.extension;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.diagnostics.extension.deployment.ClusteredDeploymentRepository;

/**
 * Handler responsible for removing the subsystem resource from the model
 *
 * @author Richard Achmatowicz (c) Red Hat 2013
 */
class ClusteringDiagnosticsSubsystemRemove extends AbstractRemoveStepHandler {

    static final ClusteringDiagnosticsSubsystemRemove INSTANCE = new ClusteringDiagnosticsSubsystemRemove();

    private ClusteringDiagnosticsSubsystemRemove() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.removeService(ClusteringDiagnosticsExtension.CLUSTER_EXTENSION_SERVICE_NAME);
        context.removeService(ClusteredDeploymentRepository.SERVICE_NAME);
    }
}
