/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.ejb;

import java.util.function.Function;

import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationBuilder;
import org.wildfly.clustering.marshalling.protostream.ImmutableSerializationContext;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamConfiguration;
import org.wildfly.clustering.marshalling.protostream.modules.ModuleClassResolver;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;

/**
 * Enumerates factories for creating a timer context marshaller, i.e. {@link jakarta.ejb.Timer#getInfo()}.
 * @author Paul Ferraro
 */
public enum TimerContextMarshallerFactory implements Function<Module, ByteBufferMarshaller> {

    JBOSS() {
        @Override
        public ByteBufferMarshaller apply(Module module) {
            return new JBossByteBufferMarshaller(MarshallingConfigurationBuilder.newInstance(ModularClassResolver.getInstance(module.getModuleLoader())).load(module.getClassLoader()).build(), module.getClassLoader());
        }
    },
    PROTOSTREAM() {
        @Override
        public ByteBufferMarshaller apply(Module module) {
            ProtoStreamConfiguration configuration = ProtoStreamConfiguration.Builder.with(new ModuleClassResolver(module)).build();
            ImmutableSerializationContext context = ImmutableSerializationContext.Builder.with(configuration).build();
            return new ProtoStreamByteBufferMarshaller(context);
        }
    },
    ;
}
