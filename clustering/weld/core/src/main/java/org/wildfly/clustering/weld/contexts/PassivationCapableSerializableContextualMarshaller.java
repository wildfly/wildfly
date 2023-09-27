/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.contexts;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.spi.PassivationCapable;

/**
 * @author Paul Ferraro
 */
public class PassivationCapableSerializableContextualMarshaller<C extends Contextual<I> & PassivationCapable, I> extends PassivationCapableSerializableMarshaller<PassivationCapableSerializableContextual<C, I>, C, I> {

    @SuppressWarnings("unchecked")
    PassivationCapableSerializableContextualMarshaller() {
        super((Class<PassivationCapableSerializableContextual<C, I>>) (Class<?>) PassivationCapableSerializableContextual.class, PassivationCapableSerializableContextual::new, PassivationCapableSerializableContextual::new, PassivationCapableSerializableContextual::getContextId);
    }
}
