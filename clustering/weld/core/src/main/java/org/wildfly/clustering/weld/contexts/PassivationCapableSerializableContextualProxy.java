/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.contexts;

import java.io.ObjectStreamException;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.spi.PassivationCapable;

import org.jboss.weld.serialization.spi.BeanIdentifier;

/**
 * @author Paul Ferraro
 */
public class PassivationCapableSerializableContextualProxy<C extends Contextual<I> & PassivationCapable, I> extends PassivationCapableSerializableProxy {
    private static final long serialVersionUID = -5640463865738900184L;

    PassivationCapableSerializableContextualProxy(String contextId, BeanIdentifier identifier) {
        super(contextId, identifier);
    }

    @SuppressWarnings("unused")
    private Object readResolve() throws ObjectStreamException {
        return new PassivationCapableSerializableContextual<>(this.getContextId(), this.getIdentifier());
    }
}
