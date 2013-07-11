package org.jboss.as.clustering.management.support.impl;

import static org.jboss.as.clustering.management.support.impl.ManagementAPIClusterSupportService.ManagementAPIConfiguration;

import org.jboss.as.clustering.impl.CoreGroupCommunicationService;
import org.jboss.as.clustering.infinispan.subsystem.ChannelDependentServiceProvider;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ManagementAPIClusterSupportServiceProvider implements ChannelDependentServiceProvider {

    public ServiceName getServiceName(String cluster) {
        return ManagementAPIClusterSupportService.getServiceName(cluster);
    }

    public ServiceController<?> install(ServiceTarget target, String cluster) {

        // install the trigger first - passive so it starts automatically when the channel starts
        ServiceName serviceName = ManagementAPIClusterSupportTriggerService.getServiceName(cluster);
        target.addService(serviceName, new ManagementAPIClusterSupportTriggerService(cluster))
                .addDependency(ChannelService.getServiceName(cluster))
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install();

        // the configuration for the ManagementAPIClusterSupportService
        final ManagementAPIConfiguration config = new ManagementAPIConfiguration(cluster);
        return target.addService(this.getServiceName(cluster), new ManagementAPIClusterSupportService(config))
                .addDependency(CoreGroupCommunicationService.getServiceName(cluster), CoreGroupCommunicationService.class, config.getCoreGroupCommunicationServiceInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();
    }
}
