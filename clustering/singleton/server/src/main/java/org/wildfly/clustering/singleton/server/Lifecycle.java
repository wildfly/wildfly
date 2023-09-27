/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

/**
 * Encapsulates behavior associated with a lifecycle sensitive object.
 * @author Paul Ferraro
 */
public interface Lifecycle {
    /**
     * Start this object.
     */
    void start();

    /**
     * Stop this object.
     */
    void stop();
}
