/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee;

/**
 * Implemented by objects that can be restarted.
 * @author Paul Ferraro
 */
public interface Restartable {

    /**
     * Starts this object.
     */
    void start();

    /**
     * Stops this object.
     */
    void stop();
}
