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

import java.util.Map;
import java.util.function.Function;

import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.marshalling.jboss.DynamicClassTable;
import org.wildfly.clustering.marshalling.jboss.ExternalizerObjectTable;

/**
 * @author Paul Ferraro
 */
public enum JBossMarshallingVersion implements Function<Map.Entry<ModuleLoader, Module>, MarshallingConfiguration> {
    VERSION_1() {
        @Override
        public MarshallingConfiguration apply(Map.Entry<ModuleLoader, Module> entry) {
            MarshallingConfiguration config = new MarshallingConfiguration();
            config.setClassResolver(ModularClassResolver.getInstance(entry.getKey()));
            config.setClassTable(new DynamicClassTable(entry.getValue().getClassLoader()));
            config.setObjectTable(new ExternalizerObjectTable(entry.getValue().getClassLoader()));
            return config;
        }
    },
    ;
    public static final JBossMarshallingVersion CURRENT = VERSION_1;
}
