/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.io.Externalizable;
import java.io.Serializable;
import java.util.function.Function;

import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.wildfly.clustering.marshalling.jboss.DynamicClassTable;
import org.wildfly.clustering.marshalling.jboss.DynamicExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.SimpleClassTable;

/**
 * @author Paul Ferraro
 */
public enum JBossMarshallingVersion implements Function<Module, MarshallingConfiguration> {

    VERSION_1() {
        @Override
        public MarshallingConfiguration apply(Module module) {
            MarshallingConfiguration config = new MarshallingConfiguration();
            config.setClassResolver(ModularClassResolver.getInstance(module.getModuleLoader()));
            config.setClassTable(new SimpleClassTable(Serializable.class, Externalizable.class));
            return config;
        }
    },
    VERSION_2() {
        @Override
        public MarshallingConfiguration apply(Module module) {
            MarshallingConfiguration config = new MarshallingConfiguration();
            config.setClassResolver(ModularClassResolver.getInstance(module.getModuleLoader()));
            config.setClassTable(new SimpleClassTable(Serializable.class, Externalizable.class));
            config.setObjectTable(new DynamicExternalizerObjectTable(module.getClassLoader()));
            return config;
        }
    },
    VERSION_3() {
        @Override
        public MarshallingConfiguration apply(Module module) {
            MarshallingConfiguration config = new MarshallingConfiguration();
            config.setClassResolver(ModularClassResolver.getInstance(module.getModuleLoader()));
            config.setClassTable(new DynamicClassTable(module.getClassLoader()));
            config.setObjectTable(new DynamicExternalizerObjectTable(module.getClassLoader()));
            return config;
        }
    },
    ;
    static final JBossMarshallingVersion CURRENT = VERSION_3;
}
