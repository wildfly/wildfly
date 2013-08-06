package org.wildfly.clustering.monitor.extension;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.wildfly.clustering.monitor.extension.deployment.ClusteredDeploymentRepository;
import org.wildfly.clustering.monitor.extension.deployment.processors.ClusteredDeploymentRepositoryProcessor;

/**
 * Handler responsible for adding the subsystem resource to the model.
 * <p/>
 * This subsystem adds a deployment processor to collect information on clustered deployments.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
class ClusterSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final ClusterSubsystemAdd INSTANCE = new ClusterSubsystemAdd();

    private ClusterSubsystemAdd() {
    }

    /*
     * Install a custom version of ClusterSubsystemRootResourceDefinition
     */
    @Override
    protected Resource createResource(OperationContext context) {
        // debugging
        assert context.getServiceRegistry(false) != null;
        // create a custom resource
        ClusterSubsystemRootResource resource = new ClusterSubsystemRootResource();
        resource.setRegistry(context.getServiceRegistry(false));
        context.addResource(PathAddress.EMPTY_ADDRESS, resource);
        return resource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
                                ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        // this subsystem gets started before jgroups and infinispan! Need to have it started after these two.
        ClusterSubsystemLogger.ROOT_LOGGER.activatingSubsystem();

        // add in deployment processors
        final boolean appclient = context.getProcessType() == ProcessType.APPLICATION_CLIENT;

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {

                // DUPs which also apply to application clients
                if (!appclient) {
                    // DUPs which apply only to
                    processorTarget.addDeploymentProcessor(ClusterExtension.SUBSYSTEM_NAME,
                            Phase.INSTALL,
                            Phase.INSTALL_CLUSTERED_DEPLOYMENT_REPOSITORY,
                            new ClusteredDeploymentRepositoryProcessor());
                }

            }
        }, OperationContext.Stage.RUNTIME);

        newControllers.add(context.getServiceTarget().addService(ClusteredDeploymentRepository.SERVICE_NAME, new ClusteredDeploymentRepository()).install());

        newControllers.add(context.getServiceTarget().addService(ClusterExtension.CLUSTER_EXTENSION_SERVICE_NAME, new ValueService<Void>(new ImmediateValue<Void>(null))).install());
    }
}