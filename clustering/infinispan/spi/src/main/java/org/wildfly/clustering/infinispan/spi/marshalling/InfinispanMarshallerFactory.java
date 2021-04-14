/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.infinispan.spi.marshalling;

import java.util.Arrays;
import java.util.HashSet;
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
        private final Set<String> protoStreamModules = new HashSet<>(Arrays.asList("org.wildfly.clustering.server", "org.wildfly.clustering.ejb.infinispan", "org.wildfly.clustering.web.infinispan"));
        private final Predicate<String> protoStreamPredicate = this.protoStreamModules::contains;

        @Override
        public Marshaller apply(ModuleLoader moduleLoader, List<Module> modules) {
            // Choose marshaller based on the associated modules
            if (modules.stream().map(Module::getName).anyMatch(this.protoStreamPredicate)) {
                return PROTOSTREAM.apply(moduleLoader, modules);
            }
            return JBOSS.apply(moduleLoader, modules);
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
