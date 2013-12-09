package org.wildfly.clustering.monitor.extension;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.monitor.extension.deployment.ClusteredDeploymentRepository;

/**
 * Handler responsible for removing the subsystem resource from the model
 *
 * @author Richard Achmatowicz (c) Red Hat 2013
 */
class ClusteringMonitorSubsystemRemove extends AbstractRemoveStepHandler {

    static final ClusteringMonitorSubsystemRemove INSTANCE = new ClusteringMonitorSubsystemRemove();

    private ClusteringMonitorSubsystemRemove() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.removeService(ClusteringMonitorExtension.CLUSTER_EXTENSION_SERVICE_NAME);
        context.removeService(ClusteredDeploymentRepository.SERVICE_NAME);
    }
}
