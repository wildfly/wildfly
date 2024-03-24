/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.delivery.parser;

import java.util.List;

import org.jboss.as.controller.xml.IntVersionSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

public enum EjbBoundMdbDeliveryMetaDataSchema implements IntVersionSchema<EjbBoundMdbDeliveryMetaDataSchema> {
    VERSION_1_0("delivery-active", 1, 0),
    VERSION_1_1("delivery-active", 1, 1),
    VERSION_1_2("delivery-active", 1, 2),
    VERSION_2_0("delivery-active", 2, 0),
    VERSION_3_0("delivery", 3, 0),
    ;
    static final EjbBoundMdbDeliveryMetaDataSchema CURRENT = VERSION_3_0;

    private final String localName;
    private final VersionedNamespace<IntVersion, EjbBoundMdbDeliveryMetaDataSchema> namespace;

    EjbBoundMdbDeliveryMetaDataSchema(String localName, int major, int minor) {
        this.localName = localName;
        this.namespace = IntVersionSchema.createURN(List.of(localName), new IntVersion(major, minor));
    }

    @Override
    public String getLocalName() {
        return localName;
    }

    @Override
    public VersionedNamespace<IntVersion, EjbBoundMdbDeliveryMetaDataSchema> getNamespace() {
        return this.namespace;
    }
}
