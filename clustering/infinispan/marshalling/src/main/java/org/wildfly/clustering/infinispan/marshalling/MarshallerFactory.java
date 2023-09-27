/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.marshalling;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.util.AggregatedClassLoader;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.infinispan.marshalling.jboss.JBossMarshaller;
import org.wildfly.clustering.infinispan.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ModuleClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;

/**
 * @author Paul Ferraro
 */
public enum MarshallerFactory implements BiFunction<ModuleLoader, List<Module>, Marshaller> {

    DEFAULT() {
        @Override
        public Marshaller apply(ModuleLoader moduleLoader, List<Module> modules) {
            SerializationContext context = ProtobufUtil.newSerializationContext();
            for (Module module : modules) {
                for (SerializationContextInitializer initializer : module.loadService(SerializationContextInitializer.class)) {
                    initializer.registerSchema(context);
                    initializer.registerMarshallers(context);
                }
            }
            return new org.infinispan.commons.marshall.ProtoStreamMarshaller(context);
        }
    },
    JBOSS() {
        @Override
        public Marshaller apply(ModuleLoader moduleLoader, List<Module> modules) {
            ClassLoader classLoader = modules.size() > 1 ? new AggregatedClassLoader(modules.stream().map(Module::getClassLoader).collect(Collectors.toList())) : modules.get(0).getClassLoader();
            return new JBossMarshaller(ModularClassResolver.getInstance(moduleLoader), classLoader);
        }
    },
    PROTOSTREAM() {
        @Override
        public Marshaller apply(ModuleLoader moduleLoader, List<Module> modules) {
            return new ProtoStreamMarshaller(new ModuleClassLoaderMarshaller(moduleLoader), new UnaryOperator<SerializationContextBuilder>() {
                @Override
                public SerializationContextBuilder apply(SerializationContextBuilder builder) {
                    for (Module module : modules) {
                        builder.load(module.getClassLoader());
                    }
                    return builder;
                }
            });
        }
    },
    ;
}
