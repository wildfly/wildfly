/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.moduleavailability.bean;

import java.util.Set;

/**
 * Interface for gaining remote access to the ModuleAvailabilityRegistrar ServiceProviderRegistry contents.
 */
public interface ModuleAvailabilityRegistrarRetriever {
    Set<String> getServices() ;
}
