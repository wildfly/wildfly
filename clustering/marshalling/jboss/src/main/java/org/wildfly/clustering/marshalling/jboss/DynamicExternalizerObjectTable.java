/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
