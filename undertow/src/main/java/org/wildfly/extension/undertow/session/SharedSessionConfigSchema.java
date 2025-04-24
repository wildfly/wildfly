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
import org.jboss.metadata.parser.servlet.Version;
import org.jboss.staxmapper.IntVersion;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Describes the supported shared-session-config XML schema versions.
 * @author Paul Ferraro
 */
public enum SharedSessionConfigSchema implements JBossAllSchema<SharedSessionConfigSchema, SharedSessionManagerConfig> {
    VERSION_1_0(1, 0, Version.SERVLET_5_0),
    VERSION_2_0(2, 0, Version.SERVLET_5_0),
    VERSION_3_0(3, 0, Version.SERVLET_6_0), // WF 37 - present
    ;
    private final VersionedNamespace<IntVersion, SharedSessionConfigSchema> namespace;
    private final Version servletVersion;

    SharedSessionConfigSchema(int major, int minor, Version servletVersion) {
        this.namespace = IntVersionSchema.createURN(List.of(IntVersionSchema.JBOSS_IDENTIFIER, this.getLocalName()), new IntVersion(major, minor));
        this.servletVersion = servletVersion;
    }

    @Override
    public String getLocalName() {
        return "shared-session-config";
    }

    @Override
    public VersionedNamespace<IntVersion, SharedSessionConfigSchema> getNamespace() {
        return this.namespace;
    }

    Version getServletVersion() {
        return this.servletVersion;
    }

    @Override
    public SharedSessionManagerConfig parse(XMLExtendedStreamReader reader, DeploymentUnit unit) throws XMLStreamException {
        return new SharedSessionConfigParser(this).parse(reader, unit);
    }
}
