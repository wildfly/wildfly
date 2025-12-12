/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.remote;

import java.util.Set;

import org.jboss.ejb.client.EJBModuleIdentifier;
import org.wildfly.clustering.server.GroupMember;

/**
 * A ModuleAvailabilityRegistrar which manages a view of deployed modules across the cluster.
 *
 * This service is used the a basis for generating module availability updates to remote clients.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public interface ModuleAvailabilityRegistrar {

    /*
     * Returns the modules currently deployed in the cluster.
     */
    Set<EJBModuleIdentifier> getServices();

    /*
     * Rturns the providers (nodes) for a given deployed module in the cluster.
     */
    Set<GroupMember> getProviders(EJBModuleIdentifier service) ;

    /*
     * Adds a ModuleAvailabilityRegistrarListener to receive callbacks on module availability-related events.
     */
    void addListener(ModuleAvailabilityRegistrarListener listener);

    /*
     * Removes a ModuleAvailabilityRegistrarListener from receiving callbacks on module availability-related events.
     */
    void removeListener(ModuleAvailabilityRegistrarListener listener);

}

