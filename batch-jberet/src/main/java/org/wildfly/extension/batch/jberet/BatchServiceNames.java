package org.wildfly.extension.batch.jberet;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.Services;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.msc.service.ServiceName;

/**
 * Service names for the batch subsystem.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchServiceNames {

    /**
     * The default service name fo the thread-pool
     */
    public static final ServiceName BASE_BATCH_THREAD_POOL_NAME = ThreadsServices.EXECUTOR.append("batch");

    /**
     * Creates a service name for the batch environment service.
     *
     * @param deploymentUnit the deployment unit to create the service name for
     *
     * @return the service name
     */
    public static ServiceName batchEnvironmentServiceName(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append("batch").append("environment");
    }

    /**
     * Creates a service name for the {@linkplain org.jberet.spi.ArtifactFactory artifact factory} service.
     *
     * @param deploymentUnit the deployment unit to create the service name for
     *
     * @return the service name
     */
    public static ServiceName batchArtifactFactoryServiceName(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append("batch").append("artifact").append("factory");
    }

    /**
     * Creates the service name used for the bean manager on the deployment.
     *
     * @param deploymentUnit the deployment unit to create the service name for
     *
     * @return the service name
     */
    public static ServiceName beanManagerServiceName(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append("beanmanager");
    }

    /**
     * Creates the service name used for the job operator registered for the deployment.
     *
     * @param deploymentUnit the deployment unit where the operator is to be registered
     *
     * @return the service name
     */
    public static ServiceName jobOperatorServiceName(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append("batch").append("job-operator");
    }

    /**
     * Creates the service name used for the job operator registered for the deployment.
     *
     * @param deploymentRuntimeName the runtime name for the deployment
     *
     * @return the service name
     */
    public static ServiceName jobOperatorServiceName(final String deploymentRuntimeName) {
        return Services.deploymentUnitName(deploymentRuntimeName).append("batch").append("job-operator");
    }

    /**
     * Creates the service name used for the job operator registered for the deployment.
     *
     * @param deploymentRuntimeName the runtime name for the deployment
     * @param subdeploymentName     the name of the subdeployment
     *
     * @return the service name
     */
    public static ServiceName jobOperatorServiceName(final String deploymentRuntimeName, final String subdeploymentName) {
        return Services.deploymentUnitName(deploymentRuntimeName, subdeploymentName).append("batch").append("job-operator");
    }
}
