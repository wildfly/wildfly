package org.jboss.as.clustering.management.support.impl;

import org.jboss.as.clustering.impl.CoreGroupCommunicationService;

/**
 * Configuration for ManagementAPIClusterSupport layer.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public interface ManagementAPIClusterSupportConfiguration {

    String getCluster() ;
    CoreGroupCommunicationService getCoreGroupCommunicationService();
}
