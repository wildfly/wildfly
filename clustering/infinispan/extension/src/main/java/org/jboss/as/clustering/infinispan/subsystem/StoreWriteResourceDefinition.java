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
public abstract class StoreWriteResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String value) {
        return PathElement.pathElement("write", value);
    }

    StoreWriteResourceDefinition(PathElement path) {
        super(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(path));
    }
}
