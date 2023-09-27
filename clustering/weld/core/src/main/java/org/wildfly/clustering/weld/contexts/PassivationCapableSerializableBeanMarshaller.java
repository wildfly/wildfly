/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.contexts;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.PassivationCapable;

/**
 * @author Paul Ferraro
 */
public class PassivationCapableSerializableBeanMarshaller<B extends Bean<I> & PassivationCapable, I> extends PassivationCapableSerializableMarshaller<PassivationCapableSerializableBean<B, I>, B, I> {

    @SuppressWarnings("unchecked")
    PassivationCapableSerializableBeanMarshaller() {
        super((Class<PassivationCapableSerializableBean<B, I>>) (Class<?>) PassivationCapableSerializableBean.class, PassivationCapableSerializableBean::new, PassivationCapableSerializableBean::new, PassivationCapableSerializableBean::getContextId);
    }
}
