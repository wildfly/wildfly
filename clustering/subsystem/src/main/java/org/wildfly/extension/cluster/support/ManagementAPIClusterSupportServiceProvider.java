package org.wildfly.extension.cluster.support;

import static org.wildfly.extension.cluster.support.ManagementAPIClusterSupportService.ManagementAPIConfiguration;

import org.jboss.as.clustering.impl.CoreGroupCommunicationService;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ManagementAPIClusterSupportServiceProvider {

    public static ServiceName getServiceName(String cluster) {
        return ManagementAPIClusterSupportService.getServiceName(cluster);
    }

    public ServiceController<?> install(ServiceTarget target, String cluster) {
        // the configuration for the ManagementAPIClusterSupportService
        final ManagementAPIConfiguration config = new ManagementAPIConfiguration(cluster);

        return target.addService(this.getServiceName(cluster), new ManagementAPIClusterSupportService(config))
                .addDependency(CoreGroupCommunicationService.getServiceName(cluster), CoreGroupCommunicationService.class, config.getCoreGroupCommunicationServiceInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();
    }

}
