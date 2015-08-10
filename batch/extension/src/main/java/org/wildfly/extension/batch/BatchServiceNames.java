package org.wildfly.extension.batch;

import org.jboss.as.server.deployment.DeploymentUnit;
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
     * Creates the service name used for the bean manager on the deployment.
     *
     * @param deploymentUnit the deployment unit to create the service name for
     *
     * @return the service name
     */
    public static ServiceName beanManagerServiceName(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append("beanmanager");
    }
}
