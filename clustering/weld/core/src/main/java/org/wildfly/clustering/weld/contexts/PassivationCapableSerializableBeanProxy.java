/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.contexts;

import java.io.ObjectStreamException;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.PassivationCapable;

import org.jboss.weld.serialization.spi.BeanIdentifier;

/**
 * @author Paul Ferraro
 */
public class PassivationCapableSerializableBeanProxy<B extends Bean<I> & PassivationCapable, I> extends PassivationCapableSerializableProxy {
    private static final long serialVersionUID = -6265576522828887136L;

    PassivationCapableSerializableBeanProxy(String contextId, BeanIdentifier identifier) {
        super(contextId, identifier);
    }

    @SuppressWarnings("unused")
    private Object readResolve() throws ObjectStreamException {
        return new PassivationCapableSerializableBean<>(this.getContextId(), this.getIdentifier());
    }
}
