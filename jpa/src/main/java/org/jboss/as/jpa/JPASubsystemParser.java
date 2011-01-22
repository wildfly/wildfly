/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jpa;

import org.jboss.as.server.ExtensionContext;
import org.jboss.as.jpa.config.JBossAssemblyDescriptor;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;

/**
 * Create a subsystem add directive from the given XML input.
 *
 * @author Scott Marlow
 */
public class JPASubsystemParser implements XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<JPASubsystemElement>>> {
    protected static final String NAMESPACE = Namespace.JPA_1_0.getUriString();

    private static final JPASubsystemParser instance = new JPASubsystemParser();

    public static JPASubsystemParser getInstance() {
        return instance;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, ParseResult<ExtensionContext.SubsystemConfiguration<JPASubsystemElement>> result)
            throws XMLStreamException {
        // parse <jboss-jpa> domain element

        JPASubsystemAdd subsystem = new JPASubsystemAdd();

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case JPA_1_0:  //  version of this subsystem (not of the JPA specification supported)
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case ASSEMBLY_DESCRIPTOR:
                            subsystem.setAssemblyDescriptor(parseAssemblyDescriptor(reader));
                            break;
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
        result.setResult(new ExtensionContext.SubsystemConfiguration<JPASubsystemElement>(subsystem));
    }

    private static JBossAssemblyDescriptor parseAssemblyDescriptor(XMLExtendedStreamReader reader) throws XMLStreamException {
        JBossAssemblyDescriptor assemblyDescriptor = new JBossAssemblyDescriptor();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case UNKNOWN:
                    ParseResult<?> result = new ParseResult<Object>();
                    reader.handleAny(result);
                    assemblyDescriptor.addAny(result.getResult());
                    break;
            }
        }
        return assemblyDescriptor;
    }
}
