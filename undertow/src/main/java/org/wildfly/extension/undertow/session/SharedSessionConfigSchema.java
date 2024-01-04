/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.xml.IntVersionSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllSchema;
import org.jboss.as.web.session.SharedSessionManagerConfig;
import org.jboss.staxmapper.IntVersion;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author Paul Ferraro
 */
public enum SharedSessionConfigSchema implements JBossAllSchema<SharedSessionConfigSchema, SharedSessionManagerConfig> {
    VERSION_1_0(1, 0),
    VERSION_2_0(2, 0),
    ;
    private final VersionedNamespace<IntVersion, SharedSessionConfigSchema> namespace;

    SharedSessionConfigSchema(int major, int minor) {
        this.namespace = IntVersionSchema.createURN(List.of(IntVersionSchema.JBOSS_IDENTIFIER, this.getLocalName()), new IntVersion(major, minor));
    }

    @Override
    public String getLocalName() {
        return "shared-session-config";
    }

    @Override
    public VersionedNamespace<IntVersion, SharedSessionConfigSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public SharedSessionManagerConfig parse(XMLExtendedStreamReader reader, DeploymentUnit unit) throws XMLStreamException {
        return new SharedSessionConfigParser(this).parse(reader, unit);
    }
}
