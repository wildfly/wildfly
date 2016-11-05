package org.wildfly.extension.clustering.singleton.deployment;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitPhaseBuilder;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.Phase;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;

/**
 * Builds a service for a sub-deployment for the next phase in the deployment chain, if configured.
 * @author Paul Ferraro
 */
public class SingletonSubDeploymentUnitPhaseBuilder implements DeploymentUnitPhaseBuilder {
    private final DeploymentUnit parent;
    private final Phase phase;

    SingletonSubDeploymentUnitPhaseBuilder(DeploymentUnit parent, Phase phase) {
        this.parent = parent;
        this.phase = phase;
    }

    @Override
    public <T> ServiceBuilder<T> build(ServiceTarget target, ServiceName name, Service<T> service) {
        // Install the actual phase service under some other name, and have it start automatically when the parent deployment's singleton service starts
        target.addService(name.append("service"), service)
                .addDependency(DeploymentUtils.getDeploymentUnitPhaseServiceName(this.parent, this.phase).append("primary"))
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install();
        // Return a dummy service builder
        return target.addService(name, new ValueService<>(service));
    }
}