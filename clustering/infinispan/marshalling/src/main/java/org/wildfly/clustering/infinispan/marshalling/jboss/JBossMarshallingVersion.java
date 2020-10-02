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

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;

import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.infinispan.marshalling.ByteBufferExternalizer;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.jboss.DefaultExternalizerProviders;
import org.wildfly.clustering.marshalling.jboss.DynamicClassTable;
import org.wildfly.clustering.marshalling.jboss.ExternalizerObjectTable;
import org.wildfly.security.manager.WildFlySecurityManager;

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
            config.setObjectTable(new ExternalizerObjectTable(loadExternalizers(entry.getValue())));
            return config;
        }
    },
    ;
    public static final JBossMarshallingVersion CURRENT = VERSION_1;

    @SuppressWarnings("unchecked")
    static List<Externalizer<Object>> loadExternalizers(Module module) {
        List<Externalizer<Object>> loadedExternalizers = WildFlySecurityManager.doUnchecked(new PrivilegedAction<List<Externalizer<Object>>>() {
            @Override
            public List<Externalizer<Object>> run() {
                List<Externalizer<Object>> externalizers = new LinkedList<>();
                for (Externalizer<Object> externalizer : ServiceLoader.load(Externalizer.class, module.getClassLoader())) {
                    externalizers.add(externalizer);
                }
                return externalizers;
            }
        });

        Set<DefaultExternalizerProviders> providers = EnumSet.allOf(DefaultExternalizerProviders.class);
        int size = loadedExternalizers.size() + 1;
        for (DefaultExternalizerProviders provider : providers) {
            size += provider.get().size();
        }
        List<Externalizer<Object>> result = new ArrayList<>(size);
        result.add((Externalizer<Object>) (Externalizer<?>) ByteBufferExternalizer.INSTANCE);
        for (DefaultExternalizerProviders provider : providers) {
            result.addAll(provider.get());
        }
        result.addAll(loadedExternalizers);
        return result;
    }
}
