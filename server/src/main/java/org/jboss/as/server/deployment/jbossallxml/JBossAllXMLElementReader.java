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

package org.jboss.as.server.deployment.jbossallxml;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 *
 * {@link XMLElementReader} that delegates to a {@link JBossAllXMLParser}
 *
 *
 * @author Stuart Douglas
 */
class JBossAllXMLElementReader implements XMLElementReader<JBossAllXmlParseContext> {

    private final JBossAllXMLParserDescription parserDescription;

    JBossAllXMLElementReader(final JBossAllXMLParserDescription<?> parserDescription) {
        this.parserDescription = parserDescription;
    }

    @Override
    public void readElement(final XMLExtendedStreamReader xmlExtendedStreamReader, final JBossAllXmlParseContext jBossXmlParseContext) throws XMLStreamException {
        final Location nsLocation = xmlExtendedStreamReader.getLocation();
        final QName elementName = xmlExtendedStreamReader.getName();
        final Object result = parserDescription.getParser().parse(xmlExtendedStreamReader, jBossXmlParseContext.getDeploymentUnit());
        jBossXmlParseContext.addResult(elementName, result, nsLocation);
    }
}
