package org.wildfly.extension.metrics.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;

import java.util.function.Supplier;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentCompleteServiceProcessor;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.metrics.MetricsSubsystemDefinition;
import org.wildfly.extension.metrics.internal.MetricCollector;

public class DeploymentMetricService implements Service {


    private final Resource rootResource;
    private final ManagementResourceRegistration managementResourceRegistration;
    private PathAddress deploymentAddress;
    private final Supplier<MetricCollector> metricCollector;
    private MetricCollector.MetricRegistration registration;

    public static void install(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit, Resource rootResource, ManagementResourceRegistration managementResourceRegistration) {
        PathAddress deploymentAddress = createDeploymentAddressPrefix(deploymentUnit);

        ServiceBuilder<?> sb = serviceTarget.addService(deploymentUnit.getServiceName().append("metrics"));
        Supplier<MetricCollector> metricCollector = sb.requires(MetricsSubsystemDefinition.WILDFLY_COLLECTOR_SERVICE);

        /*
         * The deployment metric service depends on the deployment complete service name to ensure that the metrics from
         * the deployment are collected and registered once the deployment services have all be properly installed.
         */
        sb.requires(DeploymentCompleteServiceProcessor.serviceName(deploymentUnit.getServiceName()));
        sb.setInstance(new DeploymentMetricService(rootResource, managementResourceRegistration, deploymentAddress, metricCollector))
                .install();
    }

    private DeploymentMetricService(Resource rootResource, ManagementResourceRegistration managementResourceRegistration, PathAddress deploymentAddress, Supplier<MetricCollector> metricCollector) {
        this.rootResource = rootResource;
        this.managementResourceRegistration = managementResourceRegistration;
        this.deploymentAddress = deploymentAddress;
        this.metricCollector = metricCollector;
    }

    @Override
    public void start(StartContext startContext) {
        registration = metricCollector.get().collectResourceMetrics(rootResource,
                managementResourceRegistration,
                // prepend the deployment address to the subsystem resource address
                address -> deploymentAddress.append(address));

    }

    @Override
    public void stop(StopContext stopContext) {
        registration.unregister();
    }

    private static PathAddress createDeploymentAddressPrefix(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            return PathAddress.pathAddress(DEPLOYMENT, deploymentUnit.getAttachment(Attachments.MANAGEMENT_NAME));
        } else {
            return createDeploymentAddressPrefix(deploymentUnit.getParent()).append(SUBDEPLOYMENT, deploymentUnit.getName());
        }
    }

}
