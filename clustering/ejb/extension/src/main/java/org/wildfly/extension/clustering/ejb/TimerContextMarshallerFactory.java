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
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.marshalling.protostream.modules.ModuleClassLoaderMarshaller;
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
            return new ProtoStreamByteBufferMarshaller(SerializationContextBuilder.newInstance(new ModuleClassLoaderMarshaller(module.getModuleLoader())).load(module.getClassLoader()).build());
        }
    },
    ;
}
