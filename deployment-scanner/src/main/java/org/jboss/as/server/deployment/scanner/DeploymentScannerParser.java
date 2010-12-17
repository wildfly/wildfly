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

package org.jboss.as.server.deployment.scanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import javax.xml.stream.XMLStreamException;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import static org.jboss.as.model.ParseUtils.requireNoAttributes;
import static org.jboss.as.model.ParseUtils.requireNoContent;
import static org.jboss.as.model.ParseUtils.unexpectedElement;
import org.jboss.as.server.ExtensionContext;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author John Bailey
 */
public class DeploymentScannerParser implements XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<DeploymentScannerSubsystemElement>>> {

    private static final DeploymentScannerParser INSTANCE = new DeploymentScannerParser();

    public static DeploymentScannerParser getInstance() {
        return INSTANCE;
    }

    private DeploymentScannerParser() {
    }

    public void readElement(final XMLExtendedStreamReader reader, final ParseResult<ExtensionContext.SubsystemConfiguration<DeploymentScannerSubsystemElement>> result) throws XMLStreamException {

        final List<AbstractSubsystemUpdate<DeploymentScannerSubsystemElement, ?>> updates = new ArrayList<AbstractSubsystemUpdate<DeploymentScannerSubsystemElement,?>>();

        // no attributes
        requireNoAttributes(reader);

        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DEPLOYMENT_SCANNER_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case DEPLOYMENT_SCANNER: {
                            //noinspection unchecked
                            parseScanner(reader, updates);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
        result.setResult(new ExtensionContext.SubsystemConfiguration<DeploymentScannerSubsystemElement>(new DeploymentScannerSubsystemAdd(), updates));
    }

    public void parseScanner(final XMLExtendedStreamReader reader, List<AbstractSubsystemUpdate<DeploymentScannerSubsystemElement, ?>> updates) throws XMLStreamException {

        // Handle attributes
        boolean enabled = true;
        int interval = 0;
        String path = null;
        String name = null;
        String relativeTo = null;
        final int attrCount = reader.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PATH: {
                        path = value;
                        break;
                    }
                    case NAME: {
                        name = value;
                        break;
                    }
                    case RELATIVE_TO: {
                        relativeTo = value;
                        break;
                    }
                    case SCAN_INTERVAL: {
                        interval = Integer.parseInt(value);
                        break;
                    }
                    case SCAN_ENABLED: {
                        enabled = Boolean.parseBoolean(value);
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (path == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("path"));
        }
        requireNoContent(reader);
        final DeploymentScannerAdd action = new DeploymentScannerAdd(name, path, relativeTo, interval, enabled);
        updates.add(action);
    }
}
