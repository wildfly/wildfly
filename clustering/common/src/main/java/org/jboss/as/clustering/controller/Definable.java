/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

/**
 * Implemented by objects with a definition.
 * @author Paul Ferraro
 */
public interface Definable<D> {
    /**
     * Returns the definition of this object.
     * @return this object's definition
     */
    D getDefinition();
}
