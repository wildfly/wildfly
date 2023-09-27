/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.contexts;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.spi.PassivationCapable;

import org.jboss.weld.serialization.spi.helpers.SerializableContextual;

/**
 * @author Paul Ferraro
 */
public interface PassivationCapableContextual<C extends Contextual<I> & PassivationCapable, I> extends SerializableContextual<C, I>, PassivationCapable {

    /**
     * Returns the identifier of this context.
     * @return a context identifier.
     */
    String getContextId();
}
