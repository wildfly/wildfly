/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.manager;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;

/**
 * @author Paul Ferraro
 */
public enum ManagerMarshallerProvider implements ProtoStreamMarshallerProvider {

    BEAN_MANAGER_IMPL(new BeanManagerImplMarshaller()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    ManagerMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
