/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.infinispan.commons.marshall.Marshaller;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.infinispan.marshalling.MarshallerFactory;

/**
 * @author Paul Ferraro
 */
public enum HotRodMarshallerFactory implements BiFunction<ModuleLoader, List<Module>, Marshaller> {

    LEGACY() {
        private final Set<String> protoStreamModules = Set.of("org.wildfly.clustering.web.hotrod");
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
        private final Set<String> clientModules = Set.of("org.infinispan.query.client");
        private final Predicate<String> infinispanPredicate = this.clientModules::contains;

        @Override
        public Marshaller apply(ModuleLoader moduleLoader, List<Module> modules) {
            // Use default ProtoStream marshaller if container uses remote query
            return (modules.stream().map(Module::getName).anyMatch(this.infinispanPredicate) ? MarshallerFactory.DEFAULT : MarshallerFactory.PROTOSTREAM).apply(moduleLoader, modules);
        }
    },
    ;
}
