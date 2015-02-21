package org.wildfly.extension.batch;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.Services;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.batch._private.BatchLogger;

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
     * The defined name for the thread-pool, e.g. thread-pool=batch.
     */
    public static final ServiceName BATCH_THREAD_POOL_NAME = BASE_BATCH_THREAD_POOL_NAME.append("batch");

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
     * Creates a service name for the deployment unit to define the service.
     *
     * @param deploymentUnit the deployment unit to create the service name for
     *
     * @return the service name
     */
    public static ServiceName jobXmlResolverServiceName(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append("batch").append("job-xml");
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
     * @param address the address to resolve the deployment name from
     *
     * @return the service name
     */
    public static ServiceName jobOperatorServiceName(final PathAddress address) {
        String deploymentName = null;
        String subdeploymentName = null;
        for (PathElement element : address) {
            if (ModelDescriptionConstants.DEPLOYMENT.equals(element.getKey())) {
                deploymentName = element.getValue();
            } else if (ModelDescriptionConstants.SUBDEPLOYMENT.endsWith(element.getKey())) {
                subdeploymentName = element.getValue();
            }
        }
        if (deploymentName == null) {
            throw BatchLogger.LOGGER.couldNotFindDeploymentName(address.toString());
        }
        final ServiceName result;
        if (subdeploymentName == null) {
            result = Services.deploymentUnitName(deploymentName);
        } else {
            result = Services.deploymentUnitName(deploymentName, subdeploymentName);
        }
        return result.append("batch").append("job-operator");
    }
}
