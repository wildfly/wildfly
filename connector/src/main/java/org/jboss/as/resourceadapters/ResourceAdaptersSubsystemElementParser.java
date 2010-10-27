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

package org.jboss.as.resourceadapters;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.ExtensionContext;
import org.jboss.as.model.ParseResult;
import org.jboss.jca.common.metadata.resourceadapter.ResourceAdapterParser;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import static org.jboss.as.model.ParseUtils.*;

/**
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 */
public final class ResourceAdaptersSubsystemElementParser implements
        XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<ResourceAdaptersSubsystemElement>>> {

    private static final Logger log = Logger.getLogger("org.jboss.as.resourceadapters");

    public void readElement(final XMLExtendedStreamReader reader,
            final ParseResult<ExtensionContext.SubsystemConfiguration<ResourceAdaptersSubsystemElement>> result)
            throws XMLStreamException {
        final ResourceAdaptersAdd add = new ResourceAdaptersAdd();

        try {
            String localName = null;
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case RESOURCEADAPTERS_1_0: {
                    localName = reader.getLocalName();
                    final Element element = Element.forName(reader.getLocalName());
                    log.tracef("%s -> %s", localName, element);
                    switch (element) {
                        case SUBSYSTEM: {
                            ResourceAdapterParser parser = new ResourceAdapterParser();
                            add.setResourceAdapters(parser.parse(reader));
                            requireNoContent(reader);
                            break;
                        }
                    }
                }
            }
            // }
        } catch (Exception e) {
            // log.error(reader.getText());
            throw new XMLStreamException(e);
        }

        result.setResult(new ExtensionContext.SubsystemConfiguration<ResourceAdaptersSubsystemElement>(add));
    }
}
