/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.singleton;

import java.util.function.Function;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.msc.service.ServiceName;

/**
 * Enumerates singleton runtime resource registrations.
 * @author Paul Ferraro
 */
public enum SingletonRuntimeResourceRegistration implements ResourceRegistration {
    DEPLOYMENT("deployment", name -> name.getParent().getSimpleName()),
    SERVICE("service", ServiceName::getCanonicalName),
    ;
    private final PathElement path;
    private final Function<ServiceName, String> resolver;

    SingletonRuntimeResourceRegistration(String key, Function<ServiceName, String> resolver) {
        this.path = PathElement.pathElement(key);
        this.resolver = resolver;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    PathElement pathElement(ServiceName name) {
        return PathElement.pathElement(this.path.getKey(), this.resolver.apply(name));
    }
}
