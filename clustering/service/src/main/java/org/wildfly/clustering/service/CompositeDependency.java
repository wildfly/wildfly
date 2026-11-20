/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import org.jboss.msc.service.ServiceBuilder;

/**
 * @author Paul Ferraro
 * @deprecated Superseded by {@link org.wildfly.service.Installer.Builder#requires(Iterable)}.
 */
@Deprecated(forRemoval = true)
public class CompositeDependency implements Dependency {

    private final Dependency[] dependencies;

    public CompositeDependency(Dependency... dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        for (Dependency dependency : this.dependencies) {
            if (dependency != null) {
                dependency.register(builder);
            }
        }
        return builder;
    }
}
