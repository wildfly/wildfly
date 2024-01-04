/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.contexts.beanstore;

import org.jboss.weld.contexts.beanstore.LockStore;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.ValueMarshaller;

/**
 * @author Paul Ferraro
 */
public enum BeanStoreMarshallerProvider implements ProtoStreamMarshallerProvider {
    LOCK_STORE(new ValueMarshaller<>(LockStore::new)),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    BeanStoreMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
