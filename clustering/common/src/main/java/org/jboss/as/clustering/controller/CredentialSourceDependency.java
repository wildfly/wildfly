/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.DelegatingServiceBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.credential.source.CredentialSource;

/**
 * @author Paul Ferraro
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class CredentialSourceDependency implements SupplierDependency<CredentialSource> {

    private final ExceptionSupplier<CredentialSource, Exception> supplier;
    private final Iterable<Dependency> dependencies;

    public CredentialSourceDependency(OperationContext context, Attribute attribute, ModelNode model) throws OperationFailedException {
        DependencyCollectingServiceBuilder builder = new DependencyCollectingServiceBuilder();
        this.supplier = CredentialReference.getCredentialSourceSupplier(context, (ObjectTypeAttributeDefinition) attribute.getDefinition(), model, builder);
        this.dependencies = builder;
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        for (Dependency dependency : this.dependencies) {
            dependency.register(builder);
        }
        return builder;
    }

    @Override
    public CredentialSource get() {
        try {
            return this.supplier.get();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    static class DependencyCollectingServiceBuilder extends DelegatingServiceBuilder<Object> implements Iterable<Dependency> {
        private final List<Dependency> dependencies = new LinkedList<>();

        DependencyCollectingServiceBuilder() {
            super(null);
        }

        @Override
        protected ServiceBuilder<Object> getDelegate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Dependency> iterator() {
            return this.dependencies.iterator();
        }

        @Override
        public <V> Supplier<V> requires(ServiceName name) {
            SupplierDependency<V> dependency = new ServiceSupplierDependency<>(name);
            this.dependencies.add(dependency);
            return dependency;
        }
    }
}
