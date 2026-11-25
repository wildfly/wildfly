/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumerates resource registrations for client-mappings registry providers.
 * @author Paul Ferraro
 */
public enum ClientMappingsRegistryProviderResourceRegistration implements ResourceRegistration {
    WILDCARD(PathElement.WILDCARD_VALUE),
    LOCAL("local"),
    INFINISPAN("infinispan")
    ;
    private final PathElement path;

    ClientMappingsRegistryProviderResourceRegistration(String value) {
        this.path = PathElement.pathElement("client-mappings-registry", value);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
