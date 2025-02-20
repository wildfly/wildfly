/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.remote;

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
     * Adds a ModuleAvailabilityRegistrarListener to receive callbacks on module availability-related events.
     */
    void addListener(ModuleAvailabilityRegistrarListener listener);

    /*
     * Removes a ModuleAvailabilityRegistrarListener from receiving callbacks on module availability-related events.
     */
    void removeListener(ModuleAvailabilityRegistrarListener listener);

}
