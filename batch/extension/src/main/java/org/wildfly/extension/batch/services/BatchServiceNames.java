package org.wildfly.extension.batch.services;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchServiceNames {

    public static final ServiceName BATCH_SERVICE_NAME = ServiceName.JBOSS.append("batch");

    public static ServiceName batchDeploymentServiceName(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append("batch");
    }

    public static ServiceName beanManagerServiceName(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append("beanmanager");
    }
}
