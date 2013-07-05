package org.jboss.as.clustering.management.support.impl;

import org.jboss.as.clustering.impl.ClusteringImplLogger;
import org.jboss.as.clustering.impl.CoreGroupCommunicationService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service for making RPC-based calls on a cluster.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ManagementAPIClusterSupportService implements Service<ManagementAPIClusterSupport> {

    public static ServiceName getServiceName(String cluster) {
        return CoreGroupCommunicationService.getServiceName(cluster).append("management");
    }

    private volatile ManagementAPIClusterSupport support;
    private final ManagementAPIClusterSupportConfiguration config;


    public ManagementAPIClusterSupportService(ManagementAPIClusterSupportConfiguration config) {
        this.config = config;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.support = new ManagementAPIClusterSupport(config);

        try {
            this.support.start();
        } catch(Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (this.support != null) {
            try {
                this.support.stop();
            } catch (Exception e) {
                ClusteringImplLogger.ROOT_LOGGER.managementAPIClusterSupportStopFailed(e);
            }
        }
    }

    @Override
    public ManagementAPIClusterSupport getValue() throws IllegalStateException, IllegalArgumentException {
        return this.support;
    }

    static class ManagementAPIConfiguration implements ManagementAPIClusterSupportConfiguration {

        private final String cluster ;
        private final InjectedValue<CoreGroupCommunicationService> coreGroupCommunicationService = new InjectedValue<CoreGroupCommunicationService>();

        ManagementAPIConfiguration(String cluster) {
            this.cluster = cluster;
        }

        public String getCluster() {
            return cluster;
        }

        public InjectedValue<CoreGroupCommunicationService> getCoreGroupCommunicationServiceInjector() {
            return coreGroupCommunicationService;
        }

        public CoreGroupCommunicationService getCoreGroupCommunicationService() {
            return coreGroupCommunicationService.getValue();
        }
    }
}
