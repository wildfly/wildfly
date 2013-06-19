package org.wildfly.extension.cluster;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.cluster.support.ManagementAPIClusterSupport;
import org.wildfly.extension.cluster.support.ManagementAPIClusterSupportService;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 *
 */
class ClusterSubsystemAdd extends AbstractAddStepHandler {

    static final ClusterSubsystemAdd INSTANCE = new ClusterSubsystemAdd();

    private ClusterSubsystemAdd() {
    }

    /*
     * Install a custom version of ClusterSubsystemRootResourceDefinition
     */
    @Override
    protected Resource createResource(OperationContext context) {
        // debugging
        assert context.getServiceRegistry(false) != null ;
        // create a custom resource
        ClusterSubsystemRootResource resource = new ClusterSubsystemRootResource();
        resource.setRegistry(context.getServiceRegistry(false));
        context.addResource(PathAddress.EMPTY_ADDRESS, resource);
        return resource ;
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
    }

    /** {@inheritDoc} */
    @Override
    public void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        // this subsystem gets started before jgroups and infinispan! Need to have it started after these two.
        System.out.println("Activating cluster subsystem.");

        // Because we use child resources in a read-only manner to configure the protocol stack, replace the local model with the full model
        model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        ServiceTarget target = context.getServiceTarget();
        ServiceRegistry registry = context.getServiceRegistry(false);

        // for each installed channel, install a RPC service
        for (ServiceName channelServiceName : ClusterSubsystemHelper.getAllChannelServiceNames(registry)) {

            String channelName = ClusterSubsystemHelper.getChannelNameFromChannelServiceName(channelServiceName);
            ServiceController<ManagementAPIClusterSupport> controller = ManagementAPIClusterSupportService.installManagementAPIClusterSupport(target, channelName, verificationHandler);

            // start the service for testing
            // controller.setMode(ServiceController.Mode.ACTIVE);

            // add the new service controller to the list of controllers
            if (newControllers != null) {
                newControllers.add(controller) ;
            }
        }
    }


}
