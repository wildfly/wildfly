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

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXMLParser;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parses singleton deployment configuration from XML.
 * @author Paul Ferraro
 */
public class SingletonDeploymentXMLReader implements XMLElementReader<MutableSingletonDeploymentConfiguration>, JBossAllXMLParser<SingletonDeploymentConfiguration> {

    @SuppressWarnings("unused")
    private final SingletonDeploymentSchema schema;

    public SingletonDeploymentXMLReader(SingletonDeploymentSchema schema) {
        this.schema = schema;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, MutableSingletonDeploymentConfiguration config) throws XMLStreamException {

        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            String value = reader.getAttributeValue(i);
            switch (reader.getAttributeLocalName(i)) {
                case "policy": {
                    config.setPolicy(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        ParseUtils.requireNoContent(reader);
    }

    @Override
    public SingletonDeploymentConfiguration parse(XMLExtendedStreamReader reader, DeploymentUnit unit) throws XMLStreamException {
        MutableSingletonDeploymentConfiguration configuration = new MutableSingletonDeploymentConfiguration(unit);
        this.readElement(reader, configuration);
        return configuration;
    }
}
