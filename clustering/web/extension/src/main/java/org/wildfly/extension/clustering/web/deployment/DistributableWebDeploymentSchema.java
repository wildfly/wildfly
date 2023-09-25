/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.xml.IntVersionSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.controller.xml.XMLElementSchema;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllSchema;
import org.jboss.staxmapper.IntVersion;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Enumerate the schema versions of the distibutable-web deployment descriptor.
 * @author Paul Ferraro
 */
public enum DistributableWebDeploymentSchema implements XMLElementSchema<DistributableWebDeploymentSchema, MutableDistributableWebDeploymentConfiguration>, JBossAllSchema<DistributableWebDeploymentSchema, DistributableWebDeploymentConfiguration> {

    VERSION_1_0(1, 0),
    VERSION_2_0(2, 0),
    VERSION_3_0(3, 0),
    VERSION_4_0(4, 0),
    ;
    private final VersionedNamespace<IntVersion, DistributableWebDeploymentSchema> namespace;

    DistributableWebDeploymentSchema(int major, int minor) {
        this.namespace = IntVersionSchema.createURN(List.of(IntVersionSchema.JBOSS_IDENTIFIER, this.getLocalName()), new IntVersion(major, minor));
    }

    @Override
    public String getLocalName() {
        return "distributable-web";
    }

    @Override
    public VersionedNamespace<IntVersion, DistributableWebDeploymentSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, MutableDistributableWebDeploymentConfiguration configuration) throws XMLStreamException {
        new DistributableWebDeploymentXMLReader(this).readElement(reader, configuration);
    }

    @Override
    public DistributableWebDeploymentConfiguration parse(XMLExtendedStreamReader reader, DeploymentUnit unit) throws XMLStreamException {
        MutableDistributableWebDeploymentConfiguration configuration = new MutableDistributableWebDeploymentConfiguration(unit);
        this.readElement(reader, configuration);
        return configuration;
    }
}
