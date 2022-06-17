/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.marshalling.jboss;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public class DynamicExternalizerObjectTable extends ExternalizerObjectTable {

    public DynamicExternalizerObjectTable(ClassLoader loader) {
        this(List.of(loader));
    }

    public DynamicExternalizerObjectTable(List<ClassLoader> loaders) {
        this(List.of(), loaders);
    }

    public DynamicExternalizerObjectTable(List<Externalizer<?>> externalizers, List<ClassLoader> loaders) {
        super(loadExternalizers(externalizers, loaders));
    }

    private static List<Externalizer<?>> loadExternalizers(List<Externalizer<?>> externalizers, List<ClassLoader> loaders) {
        List<Externalizer<?>> loadedExternalizers = WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @Override
            public List<Externalizer<?>> run() {
                List<Externalizer<?>> externalizers = new LinkedList<>();
                for (ClassLoader loader : loaders) {
                    for (Externalizer<? super Object> externalizer : ServiceLoader.load(Externalizer.class, loader)) {
                        externalizers.add(externalizer);
                    }
                }
                return externalizers;
            }
        });

        Set<DefaultExternalizerProviders> providers = EnumSet.allOf(DefaultExternalizerProviders.class);
        int size = loadedExternalizers.size();
        for (DefaultExternalizerProviders provider : providers) {
            size += provider.get().size();
        }
        List<Externalizer<?>> result = new ArrayList<>(size);
        // Add static externalizers first
        result.addAll(externalizers);
        // Then default externalizers
        for (DefaultExternalizerProviders provider : providers) {
            result.addAll(provider.get());
        }
        // Then loaded externalizers
        result.addAll(loadedExternalizers);
        return result;
    }
}
