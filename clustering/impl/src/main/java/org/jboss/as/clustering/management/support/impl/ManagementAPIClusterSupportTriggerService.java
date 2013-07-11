package org.jboss.as.clustering.management.support.impl;

import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service for triggering the start of the ManagementAPIClusterSupport service.
 * This is done by:
 * - making the service dependent on ChannelService only
 * - setting the mode to PASSIVE so that it starts whenever its dependency is started
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ManagementAPIClusterSupportTriggerService implements Service<Object> {

    public static ServiceName getServiceName(String cluster) {
        return ManagementAPIClusterSupportService.getServiceName(cluster).append("trigger");
    }

    private final String cluster;

    public ManagementAPIClusterSupportTriggerService(String cluster) {
        this.cluster = cluster;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceController<ManagementAPIClusterSupport> controller = getController(this.cluster);
        try {
            controller.setMode(ServiceController.Mode.ACTIVE);
        } catch(Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        ServiceController<ManagementAPIClusterSupport> controller = getController(this.cluster);
        if (controller != null) {
            try {
                controller.setMode(ServiceController.Mode.REMOVE);
            } catch (Exception e) {
                System.out.println("remove");
            }
        }
    }

    @Override
    public Object getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    private ServiceController<ManagementAPIClusterSupport> getController(String cluster) {
        ServiceName rpcServiceName = ManagementAPIClusterSupportService.getServiceName(cluster);
        ServiceContainer registry = ServiceContainerHelper.getCurrentServiceContainer();

        ServiceController<ManagementAPIClusterSupport> controller = null;
        try {
             controller = ServiceContainerHelper.getService(registry, rpcServiceName);
        } catch(ServiceNotFoundException snf) {
             // this exception will arise
        }

        return controller;
    }
}
