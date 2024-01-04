/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee;

/**
 * Handles batch context switching.
 * @author Paul Ferraro
 */
public interface BatchContext extends AutoCloseable {
    /**
     * Closes this batch context.
     */
    @Override
    void close();
}
