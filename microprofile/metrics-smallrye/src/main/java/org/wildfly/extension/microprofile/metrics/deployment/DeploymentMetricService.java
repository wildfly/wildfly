package org.wildfly.extension.microprofile.metrics.deployment;

import static org.eclipse.microprofile.metrics.MetricRegistry.Type.APPLICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import io.smallrye.metrics.MetricRegistries;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.DeploymentCompleteServiceProcessor;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.microprofile.metrics.MetricsRegistrationService;
import org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition;

public class DeploymentMetricService implements Service {


    private final Resource rootResource;
    private final ManagementResourceRegistration managementResourceRegistration;
    private PathAddress deploymentAddress;
    private final Supplier<MetricsRegistrationService> registrationService;
    private Set<String> registeredMetrics = new HashSet<>();

    public static void install(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit, Resource rootResource, ManagementResourceRegistration managementResourceRegistration) {
        PathAddress deploymentAddress = createDeploymentAddressPrefix(deploymentUnit);

        ServiceBuilder<?> sb = serviceTarget.addService(deploymentUnit.getServiceName().append("metrics"));
        Supplier<MetricsRegistrationService> registrationService = sb.requires(MicroProfileMetricsSubsystemDefinition.WILDFLY_REGISTRATION_SERVICE);

        /*
         * The deployment metric service depends on the deployment complete service name to ensure that the metrics from
         * the deployment are collected and registered once the deployment services have all be properly installed.
         */
        sb.requires(DeploymentCompleteServiceProcessor.serviceName(deploymentUnit.getServiceName()));
        sb.setInstance(new DeploymentMetricService(rootResource, managementResourceRegistration, deploymentAddress, registrationService))
                .install();
    }

    private DeploymentMetricService(Resource rootResource, ManagementResourceRegistration managementResourceRegistration, PathAddress deploymentAddress, Supplier<MetricsRegistrationService> registrationService) {
        this.rootResource = rootResource;
        this.managementResourceRegistration = managementResourceRegistration;
        this.deploymentAddress = deploymentAddress;
        this.registrationService = registrationService;
    }

    @Override
    public void start(StartContext startContext) {
        MetricRegistry applicationRegistry = MetricRegistries.get(APPLICATION);
        registeredMetrics = registrationService.get().registerMetrics(rootResource, managementResourceRegistration, applicationRegistry,
                new MetricsRegistrationService.MetricNameCreator() {
                    @Override
                    public String createName(PathAddress address, String attributeName) {
                        return deploymentAddress.append(address).toPathStyleString().substring(1) + "/" + attributeName;
                    }

                    @Override
                    public PathAddress getResourceAddress(PathAddress address) {
                        return deploymentAddress.append(address);
                    }
                });
    }

    @Override
    public void stop(StopContext stopContext) {
        if (registeredMetrics != null) {
            MetricRegistry applicationRegistry = MetricRegistries.get(APPLICATION);
            for (String registeredMetric : registeredMetrics) {
                applicationRegistry.remove(registeredMetric);
            }
            registeredMetrics = null;
        }
    }

    private static PathAddress createDeploymentAddressPrefix(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            return PathAddress.pathAddress(DEPLOYMENT, deploymentUnit.getName());
        } else {
            return createDeploymentAddressPrefix(deploymentUnit.getParent()).append(SUBDEPLOYMENT, deploymentUnit.getName());
        }
    }

}
