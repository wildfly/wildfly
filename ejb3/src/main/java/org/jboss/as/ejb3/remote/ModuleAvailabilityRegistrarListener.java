/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.remote;

import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.wildfly.clustering.server.GroupMember;

import java.util.List;
import java.util.Map;

/**
 * Listener class that notifies on cluster-wide module availability changes.
 *
 * @author Richard Achmatowicz
 */
public interface ModuleAvailabilityRegistrarListener {

    /*
     * Provides a map of available modules, and the nodes they reside on, which are deployed on servers in a cluster.
     */
    void modulesAvailable(Map<DeploymentModuleIdentifier, List<GroupMember>> modules) ;

    /*
     * Provides a map of unavailable modules, and the nodes they are no longer reside on,which have been undeployed on
     * servers in a cluster.
     */
    void modulesUnavailable(Map<DeploymentModuleIdentifier, List<GroupMember>> modules) ;

}

