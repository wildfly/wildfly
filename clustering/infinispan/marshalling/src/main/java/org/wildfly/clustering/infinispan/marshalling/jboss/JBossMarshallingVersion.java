/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.marshalling.jboss;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.MarshallingConfiguration;
import org.wildfly.clustering.infinispan.marshalling.ByteBufferExternalizer;
import org.wildfly.clustering.marshalling.jboss.DynamicClassTable;
import org.wildfly.clustering.marshalling.jboss.DynamicExternalizerObjectTable;

/**
 * @author Paul Ferraro
 */
public enum JBossMarshallingVersion implements Function<Map.Entry<ClassResolver, ClassLoader>, MarshallingConfiguration> {
    VERSION_1() {
        @Override
        public MarshallingConfiguration apply(Map.Entry<ClassResolver, ClassLoader> entry) {
            MarshallingConfiguration config = new MarshallingConfiguration();
            ClassLoader loader = entry.getValue();
            config.setClassResolver(entry.getKey());
            config.setClassTable(new DynamicClassTable(loader));
            config.setObjectTable(new DynamicExternalizerObjectTable(List.of(ByteBufferExternalizer.INSTANCE), List.of(loader)));
            return config;
        }
    },
    ;
    public static final JBossMarshallingVersion CURRENT = VERSION_1;
}
