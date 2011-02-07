/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.as.server.ExtensionContext;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The webservices subsystem parser.
 *
 * @author alessio.soldano@jboss.com
 * @since 08-Nov-2010
 */
public class WSSubsystemParser implements XMLStreamConstants, XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<WSSubsystemElement>>> {

    private static final WSSubsystemParser INSTANCE = new WSSubsystemParser();
    private static final Logger log = Logger.getLogger("org.jboss.as.webservices");

    public static WSSubsystemParser getInstance() {
        return INSTANCE;
    }

    private WSSubsystemParser() {
        //NOP
    }

    /** {@inheritDoc} */
    public void readElement(XMLExtendedStreamReader reader, ParseResult<ExtensionContext.SubsystemConfiguration<WSSubsystemElement>> result) throws XMLStreamException {
        final WSSubsystemAdd add = new WSSubsystemAdd();
        try {
            String localName = null;
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEBSERVICES_1_0: {
                    localName = reader.getLocalName();
                    final Element element = Element.forName(localName);
                    log.tracef("%s -> %s", localName, element);
                    switch (element) {
                        case SUBSYSTEM: {
                            add.setConfiguration(WSConfigurationParser.parse(reader));
                            ParseUtils.requireNoContent(reader);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new XMLStreamException(e);
        }

        result.setResult(new ExtensionContext.SubsystemConfiguration<WSSubsystemElement>(add)); //updates to be added here
    }

}
