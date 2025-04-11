/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.service;

import java.util.Collections;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.BiFunction;

import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Provides service installers of a given type.
 * @author Paul Ferraro
 */
public class ProvidedBiServiceInstallerProvider<P extends BiFunction<String, String, Iterable<ServiceInstaller>>> implements BiFunction<String, String, Iterable<ServiceInstaller>> {

    private final Class<P> providerType;
    private final ClassLoader loader;

    public ProvidedBiServiceInstallerProvider(Class<P> providerType, ClassLoader loader) {
        this.providerType = providerType;
        this.loader = loader;
    }

    @Override
    public Iterable<ServiceInstaller> apply(String value, String context) {
        Class<P> providerType = this.providerType;
        ClassLoader loader = this.loader;
        return new Iterable<> () {
            @Override
            public Iterator<ServiceInstaller> iterator() {
                return new Iterator<>() {
                    private final Iterator<P> providers = ServiceLoader.load(providerType, loader).iterator();
                    private Iterator<ServiceInstaller> installers = Collections.emptyIterator();

                    @Override
                    public boolean hasNext() {
                        return this.providers.hasNext() || this.installers.hasNext();
                    }

                    @Override
                    public ServiceInstaller next() {
                        while (!this.installers.hasNext()) {
                            this.installers = this.providers.next().apply(value, context).iterator();
                        }
                        return this.installers.next();
                    }
                };
            }
        };
    }
}
