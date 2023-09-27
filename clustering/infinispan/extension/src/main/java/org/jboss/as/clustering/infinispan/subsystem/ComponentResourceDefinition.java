/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.controller.PathElement;

/**
 * @author Paul Ferraro
 */
public abstract class ComponentResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("component", name);
    }

    public ComponentResourceDefinition(PathElement path) {
        super(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(path));
    }
}
