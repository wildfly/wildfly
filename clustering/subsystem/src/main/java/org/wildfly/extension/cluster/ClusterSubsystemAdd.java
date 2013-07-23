package org.wildfly.extension.cluster;

import static org.wildfly.extension.cluster.ClusterSubsystemLogger.ROOT_LOGGER;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;

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
        ROOT_LOGGER.activatingSubsystem();

        newControllers.add(context.getServiceTarget().addService(ClusterExtension.CLUSTER_EXTENSION_SERVICE_NAME, new ValueService<Void>(new ImmediateValue<Void>(null))).install());
    }
}