/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
