/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.singleton.deployment;

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
 * Enumerates the singleton deployment configuration schemas.
 * @author Paul Ferraro
 */
public enum SingletonDeploymentSchema implements XMLElementSchema<SingletonDeploymentSchema, MutableSingletonDeploymentConfiguration>, JBossAllSchema<SingletonDeploymentSchema, SingletonDeploymentConfiguration> {

    VERSION_1_0(1, 0),
    ;
    public static final SingletonDeploymentSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, SingletonDeploymentSchema> namespace;

    SingletonDeploymentSchema(int major, int minor) {
        this.namespace = IntVersionSchema.createURN(List.of(IntVersionSchema.JBOSS_IDENTIFIER, this.getLocalName()), new IntVersion(major, minor));
    }

    @Override
    public String getLocalName() {
        return "singleton-deployment";
    }

    @Override
    public VersionedNamespace<IntVersion, SingletonDeploymentSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, MutableSingletonDeploymentConfiguration configuration) throws XMLStreamException {
        new SingletonDeploymentXMLReader(this).readElement(reader, configuration);
    }

    @Override
    public SingletonDeploymentConfiguration parse(XMLExtendedStreamReader reader, DeploymentUnit unit) throws XMLStreamException {
        MutableSingletonDeploymentConfiguration configuration = new MutableSingletonDeploymentConfiguration(unit);
        this.readElement(reader, configuration);
        return configuration;
    }
}
