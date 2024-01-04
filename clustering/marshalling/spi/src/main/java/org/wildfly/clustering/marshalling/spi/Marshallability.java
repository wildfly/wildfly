/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

/**
 * @author Paul Ferraro
 */
public interface Marshallability {
    /**
     * Indicates whether the specified object can be marshalled.
     * @param object an object to be marshalled
     * @return true, if the specified object can be marshalled, false otherwise
     */
    boolean isMarshallable(Object object);
}
