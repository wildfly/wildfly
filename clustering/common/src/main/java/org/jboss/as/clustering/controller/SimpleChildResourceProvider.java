/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Set;
import java.util.function.Supplier;

import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.common.function.Functions;

/**
 * A simple {@link ChildResourceProvider} containing a predefined set of children.
 * @author Paul Ferraro
 */
public class SimpleChildResourceProvider implements ChildResourceProvider {

    private final Set<String> children;
    private final Supplier<Resource> provider;

    public SimpleChildResourceProvider(Set<String> children) {
        this(children, Functions.constantSupplier(PlaceholderResource.INSTANCE));
    }

    public SimpleChildResourceProvider(Set<String> children, Supplier<Resource> provider) {
        this.children = children;
        this.provider = provider;
    }

    @Override
    public Resource getChild(String name) {
        return this.children.contains(name) ? this.provider.get() : null;
    }

    @Override
    public Set<String> getChildren() {
        return this.children;
    }
}
