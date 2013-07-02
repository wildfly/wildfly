package org.wildfly.extension.cluster;

import org.jboss.as.clustering.management.support.impl.ManagementAPIClusterSupportServiceProvider;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

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
        // remove all RPC services installed for channels
        ServiceRegistry registry = context.getServiceRegistry(false);
        // get all channel service names, whether UP or not
        for (ServiceName channelServiceName : ClusterSubsystemHelper.getAllChannelServiceNames(registry)) {

            String channelName = ClusterSubsystemHelper.getChannelNameFromChannelServiceName(channelServiceName);
            ServiceName managementSupportServiceName = (new ManagementAPIClusterSupportServiceProvider()).getServiceName(channelName);
            context.removeService(managementSupportServiceName);
        }
    }
}
