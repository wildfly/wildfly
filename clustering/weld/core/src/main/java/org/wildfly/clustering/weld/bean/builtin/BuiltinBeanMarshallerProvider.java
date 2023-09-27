/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.bean.builtin;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.jboss.weld.bean.builtin.InstanceImpl;
import org.jboss.weld.manager.BeanManagerImpl;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.reflect.TernaryMethodMarshaller;

/**
 * @author Paul Ferraro
 */
public enum BuiltinBeanMarshallerProvider implements ProtoStreamMarshallerProvider {

    @SuppressWarnings("unchecked")
    INSTANCE_IMPL(new TernaryMethodMarshaller<>(InstanceImpl.class, InjectionPoint.class, CreationalContext.class, BeanManagerImpl.class, (injectionPoint, context, manager) -> InstanceImpl.of(injectionPoint, context, manager))),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    BuiltinBeanMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
