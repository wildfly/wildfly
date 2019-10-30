/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors;

import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ee.structure.JBossDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXMLParser;
import org.jboss.metadata.ejb.parser.jboss.ejb3.JBossEjb3MetaDataParser;
import org.jboss.metadata.ejb.parser.spec.AbstractMetaDataParser;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.EjbJarVersion;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The app client handler for jboss-all.xml
 *
 * @author Stuart Douglas
 */
public class EjbJarJBossAllParser implements JBossAllXMLParser<EjbJarMetaData> {

    public static final AttachmentKey<EjbJarMetaData> ATTACHMENT_KEY = AttachmentKey.create(EjbJarMetaData.class);

    public static final QName ROOT_ELEMENT = new QName("http://www.jboss.com/xml/ns/javaee", "ejb-jar");

    @Override
    public EjbJarMetaData parse(final XMLExtendedStreamReader reader, final DeploymentUnit deploymentUnit) throws XMLStreamException {
        return new EjbJarParser(EjbJarParsingDeploymentUnitProcessor.createJbossEjbJarParsers()).parse(reader, JBossDescriptorPropertyReplacement.propertyReplacer(deploymentUnit));
    }

    //TODO: move this to JBMETA
    private static final class EjbJarParser extends JBossEjb3MetaDataParser {

        public EjbJarParser(Map<String, AbstractMetaDataParser<?>> parsers) {
            super(parsers);
        }

        @Override
        public EjbJarMetaData parse(final XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
            final EjbJarMetaData metaData = new EjbJarMetaData(EjbJarVersion.EJB_3_1);
            processAttributes(metaData, reader);
            processElements(metaData, reader, propertyReplacer);
            return metaData;
        }
    }

}
