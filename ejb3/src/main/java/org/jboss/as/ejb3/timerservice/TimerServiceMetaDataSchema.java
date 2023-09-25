/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice;

import java.util.List;

import org.jboss.as.controller.xml.IntVersionSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * @author Paul Ferraro
 */
public enum TimerServiceMetaDataSchema implements IntVersionSchema<TimerServiceMetaDataSchema> {
    VERSION_1_0(1, 0),
    VERSION_2_0(2, 0),
    ;
    static final TimerServiceMetaDataSchema CURRENT = VERSION_2_0;

    private final VersionedNamespace<IntVersion, TimerServiceMetaDataSchema> namespace;

    TimerServiceMetaDataSchema(int major, int minor) {
        this.namespace = IntVersionSchema.createURN(List.of(this.getLocalName()), new IntVersion(major, minor));
    }

    @Override
    public String getLocalName() {
        return "timer-service";
    }

    @Override
    public VersionedNamespace<IntVersion, TimerServiceMetaDataSchema> getNamespace() {
        return this.namespace;
    }
}
