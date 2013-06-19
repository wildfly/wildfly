package org.wildfly.extension.cluster.support;

import org.jboss.as.clustering.impl.CoreGroupCommunicationService;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.cluster.ClusterExtension;

/**
 * Service for making RPC-based calls on a cluster.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ManagementAPIClusterSupportService implements Service<ManagementAPIClusterSupport> {

    public static ServiceName getServiceName(String cluster) {
        return CoreGroupCommunicationService.getServiceName(cluster).append("management");
    }

    private volatile ManagementAPIClusterSupport support;
    private final ManagementAPIClusterSupportConfiguration config;


    public ManagementAPIClusterSupportService(ManagementAPIClusterSupportConfiguration config) {
        this.config = config;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.support = new ManagementAPIClusterSupport(config);

        try {
            this.support.start();
        } catch(Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (this.support != null) {
            try {
                this.support.stop();
            } catch (Exception e) {
                System.out.println("can't stop");
            }
        }
    }

    @Override
    public ManagementAPIClusterSupport getValue() throws IllegalStateException, IllegalArgumentException {
        return this.support;
    }

    static class ManagementAPIConfiguration implements ManagementAPIClusterSupportConfiguration {

        private final String cluster ;
        private final InjectedValue<CoreGroupCommunicationService> coreGroupCommunicationService = new InjectedValue<CoreGroupCommunicationService>();

        ManagementAPIConfiguration(String cluster) {
            this.cluster = cluster;
        }

        public String getCluster() {
            return cluster;
        }

        public InjectedValue<CoreGroupCommunicationService> getCoreGroupCommunicationServiceInjector() {
            return coreGroupCommunicationService;
        }

        public CoreGroupCommunicationService getCoreGroupCommunicationService() {
            return coreGroupCommunicationService.getValue();
        }
    }

    public static ServiceController<ManagementAPIClusterSupport> installManagementAPIClusterSupport(ServiceTarget target, String channelName, ServiceVerificationHandler verificationHandler) {
        ManagementAPIClusterSupportServiceProvider provider = new ManagementAPIClusterSupportServiceProvider();
        ServiceController<ManagementAPIClusterSupport> controller =  (ServiceController<ManagementAPIClusterSupport>) provider.install(target, channelName) ;
        return controller ;
    }

    // below here is new

    public static void addManagementAPIClusterSupportInstallationStep(OperationContext context, String channelName) {

        // set up the operation for the step
        PathAddress rootSubsystemAddress = PathAddress.pathAddress(ClusterExtension.SUBSYSTEM_PATH);
        ModelNode registerManagementSupportOp = Util.createOperation("read-resource", rootSubsystemAddress);
        registerManagementSupportOp.get("channel").set(new ModelNode(channelName));

        // add the step
        context.addStep(registerManagementSupportOp, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                // this is a noop - if we can read the resource, we know the subsystem is installed
                String channelName = operation.get("channel").asString();
                context.completeStep(new ManagementResultHandler(channelName));
            }
        }, OperationContext.Stage.RUNTIME);
    }

    public static class  ManagementResultHandler implements OperationContext.ResultHandler {
        private final String channelName;

        public ManagementResultHandler(String channelName) {
            this.channelName = channelName;
        }

        @Override
        public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
            if (resultAction.equals(OperationContext.ResultAction.KEEP)) {
                System.out.println("cluster subsystem is present");
            } else {
                System.out.println("cluster subsystem is not present");
            }
        }
    }
}
