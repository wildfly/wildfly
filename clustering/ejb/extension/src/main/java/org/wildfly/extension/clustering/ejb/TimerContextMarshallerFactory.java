/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.ejb;

import java.util.function.Function;

import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.wildfly.clustering.marshalling.jboss.DynamicExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.protostream.ModuleClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * @author Paul Ferraro
 */
public enum TimerContextMarshallerFactory implements Function<Module, ByteBufferMarshaller> {

    JBOSS() {
        @Override
        public ByteBufferMarshaller apply(Module module) {
            MarshallingConfiguration config = new MarshallingConfiguration();
            config.setClassResolver(ModularClassResolver.getInstance(module.getModuleLoader()));
            config.setObjectTable(new DynamicExternalizerObjectTable(module.getClassLoader()));
            return new JBossByteBufferMarshaller(new SimpleMarshallingConfigurationRepository(config), module.getClassLoader());
        }
    },
    PROTOSTREAM() {
        @Override
        public ByteBufferMarshaller apply(Module module) {
            SerializationContextBuilder builder = new SerializationContextBuilder(new ModuleClassLoaderMarshaller(module.getModuleLoader())).load(module.getClassLoader());
            return new ProtoStreamByteBufferMarshaller(builder.build());
        }
    },
    ;
}
