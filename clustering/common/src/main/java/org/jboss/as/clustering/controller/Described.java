/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

/**
 * Exposes the descriptor of this object.
 * @author Paul Ferraro
 */
public interface Described<D> {
    /**
     * Returns the descriptor of this object
     * @return the descriptor of this object
     */
    D getDescriptor();
}
