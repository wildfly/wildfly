/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.marshall;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.infinispan.commons.marshall.Marshaller;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.infinispan.marshalling.MarshallerFactory;
import org.wildfly.clustering.marshalling.protostream.ModuleClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;

/**
 * @author Paul Ferraro
 */
public enum InfinispanMarshallerFactory implements BiFunction<ModuleLoader, List<Module>, Marshaller> {

    LEGACY() {
        private final Set<String> protoStreamModules = Set.of("org.wildfly.clustering.server", "org.wildfly.clustering.ejb.infinispan", "org.wildfly.clustering.web.infinispan");
        private final Predicate<String> protoStreamPredicate = this.protoStreamModules::contains;

        @Override
        public Marshaller apply(ModuleLoader moduleLoader, List<Module> modules) {
            // Choose marshaller based on the associated modules
            return (modules.stream().map(Module::getName).anyMatch(this.protoStreamPredicate) ? PROTOSTREAM : JBOSS).apply(moduleLoader, modules);
        }
    },
    JBOSS() {
        @Override
        public Marshaller apply(ModuleLoader moduleLoader, List<Module> modules) {
            return MarshallerFactory.JBOSS.apply(moduleLoader, modules);
        }
    },
    PROTOSTREAM() {
        @Override
        public Marshaller apply(ModuleLoader moduleLoader, List<Module> modules) {
            return new InfinispanProtoStreamMarshaller(new ModuleClassLoaderMarshaller(moduleLoader), new UnaryOperator<SerializationContextBuilder>() {
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
