/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.bean.builtin;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.jboss.weld.bean.builtin.InstanceImpl;
import org.jboss.weld.manager.BeanManagerImpl;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.reflect.TernaryMethodMarshaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class BuiltinBeanSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public BuiltinBeanSerializationContextInitializer() {
        super(InstanceImpl.class.getPackage());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new TernaryMethodMarshaller<>(InstanceImpl.class, InjectionPoint.class, CreationalContext.class, BeanManagerImpl.class, (injectionPoint, ctx, manager) -> InstanceImpl.of(injectionPoint, ctx, manager)));
    }
}
