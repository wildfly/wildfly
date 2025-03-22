/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.Function;

import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationBuilder;

/**
 * Enumerates JBoss Marshalling configuration versions.
 * @author Paul Ferraro
 */
public enum JBossMarshallingVersion implements Function<Module, MarshallingConfiguration> {

    @Deprecated VERSION_1() {
        @Override
        public MarshallingConfiguration apply(Module module) {
            return MarshallingConfigurationBuilder.newInstance(ModularClassResolver.getInstance(module.getModuleLoader())).build();
        }
    },
    @Deprecated VERSION_2() {
        @Override
        public MarshallingConfiguration apply(Module module) {
            MarshallingConfigurationBuilder builder = MarshallingConfigurationBuilder.newInstance(ModularClassResolver.getInstance(module.getModuleLoader()));
            return new org.wildfly.clustering.marshalling.jboss.externalizer.LegacyExternalizerConfiguratorFactory(module.getClassLoader()).apply(builder).build();
        }
    },
    VERSION_3() {
        @Override
        public MarshallingConfiguration apply(Module module) {
            MarshallingConfigurationBuilder builder = MarshallingConfigurationBuilder.newInstance(ModularClassResolver.getInstance(module.getModuleLoader())).load(module.getClassLoader());
            return new org.wildfly.clustering.marshalling.jboss.externalizer.LegacyExternalizerConfiguratorFactory(module.getClassLoader()).apply(builder).build();
        }
    },
    ;
    static final JBossMarshallingVersion CURRENT = VERSION_3;
}
