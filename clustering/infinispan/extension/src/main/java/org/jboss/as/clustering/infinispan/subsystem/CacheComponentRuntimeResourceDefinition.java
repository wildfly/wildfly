/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author Paul Ferraro
 */
public class CacheComponentRuntimeResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static final PathElement pathElement(String name) {
        return PathElement.pathElement("component", name);
    }

    CacheComponentRuntimeResourceDefinition(PathElement path) {
        this(path, path);
    }

    CacheComponentRuntimeResourceDefinition(PathElement path, PathElement resolverPath) {
        super(new Parameters(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(resolverPath)).setRuntime());
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        return parent.registerSubModel(this);
    }
}
