/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.clustering;

import java.util.List;

import org.jboss.as.controller.xml.IntVersionSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * @author Paul Ferraro
 */
public enum ClusteringSchema implements IntVersionSchema<ClusteringSchema> {

    VERSION_1_0(1, 0),
    VERSION_1_1(1, 1),
    ;
    static final ClusteringSchema CURRENT = VERSION_1_1;

    private final VersionedNamespace<IntVersion, ClusteringSchema> namespace;

    ClusteringSchema(int major, int minor) {
        this.namespace = IntVersionSchema.createURN(List.of(this.getLocalName()), new IntVersion(major, minor));
    }

    @Override
    public String getLocalName() {
        return "clustering";
    }

    @Override
    public VersionedNamespace<IntVersion, ClusteringSchema> getNamespace() {
        return this.namespace;
    }
}
