/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.remote;

import org.jboss.ejb.client.Affinity;

/**
 * Defines the affinity requirements for remote clients.
 *
 * @author Paul Ferraro
 *
 * @param <I> the bean type
 */
public interface AffinitySupport<I> {
    /**
     * Returns the strong affinity for all invocations.
     * Strong affinity indicates a strict load balancing requirement.
     * @return an affinity
     */
    Affinity getStrongAffinity();

    /**
     * Returns the weak affinity of the specified bean identifier.
     * Weak affinity indicates a load balancing preference within the confines of the strong affinity.
     * @param id a bean identifier
     * @return an affinity
     */
    Affinity getWeakAffinity(I id);
}
