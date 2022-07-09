/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
