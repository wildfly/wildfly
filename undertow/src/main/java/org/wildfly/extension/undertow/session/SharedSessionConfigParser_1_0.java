/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.session;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.ee.structure.JBossDescriptorPropertyReplacement;
import org.jboss.as.server.logging.ServerLogger;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXMLParser;
import org.jboss.metadata.parser.jbossweb.ReplicationConfigParser;
import org.jboss.metadata.parser.servlet.SessionConfigMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Parse shared session manager config
 *
 * @author Stuart Douglas
 */
public class SharedSessionConfigParser_1_0 implements JBossAllXMLParser<SharedSessionManagerConfig> {

    public static final String NAMESPACE_1_0 = "urn:jboss:shared-session-config:1.0";
    public static final QName ROOT_ELEMENT = new QName(NAMESPACE_1_0, "shared-session-config");

    public static final SharedSessionConfigParser_1_0 INSTANCE = new SharedSessionConfigParser_1_0();

    @Override
    public SharedSessionManagerConfig parse(XMLExtendedStreamReader reader, DeploymentUnit deploymentUnit) throws XMLStreamException {
        if(deploymentUnit.getParent() != null) {
            UndertowLogger.ROOT_LOGGER.sharedSessionConfigNotInRootDeployment(deploymentUnit.getName());
            return null;
        }
        SharedSessionManagerConfig result = new SharedSessionManagerConfig();
        PropertyReplacer propertyReplacer = JBossDescriptorPropertyReplacement.propertyReplacer(deploymentUnit);

        readElement(reader, result, propertyReplacer);
        return result;
    }

    enum Element {
        MAX_ACTIVE_SESSIONS,
        REPLICATION_CONFIG,
        SESSION_CONFIG,

        // default unknown element
        UNKNOWN;

        private static final Map<QName, Element> elements;

        static {
            Map<QName, Element> elementsMap = new HashMap<QName, Element>();
            elementsMap.put(new QName(NAMESPACE_1_0, "max-active-sessions"), Element.MAX_ACTIVE_SESSIONS);
            elementsMap.put(new QName(NAMESPACE_1_0, "replication-config"), Element.REPLICATION_CONFIG);
            elementsMap.put(new QName(NAMESPACE_1_0, "session-config"), Element.SESSION_CONFIG);
            elements = elementsMap;
        }

        static Element of(QName qName) {
            QName name;
            if (qName.getNamespaceURI().equals("")) {
                name = new QName(NAMESPACE_1_0, qName.getLocalPart());
            } else {
                name = qName;
            }
            final Element element = elements.get(name);
            return element == null ? UNKNOWN : element;
        }
    }


    enum Version {
        UNDERTOW_SHARED_1_0,
        UNKNOWN
    }

    private SharedSessionConfigParser_1_0() {
    }

    public void readElement(final XMLExtendedStreamReader reader, final SharedSessionManagerConfig result, PropertyReplacer propertyReplacer) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        if (count != 0) {
            throw ParseUtils.unexpectedAttribute(reader, 0);
        }
        // xsd:sequence
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case MAX_ACTIVE_SESSIONS:
                            final String value = getElementText(reader, propertyReplacer);
                            result.setMaxActiveSessions(Integer.parseInt(value));
                            break;
                        case REPLICATION_CONFIG:
                            result.setReplicationConfig(ReplicationConfigParser.parse(reader, propertyReplacer));
                            break;
                        case SESSION_CONFIG:
                            result.setSessionConfig(SessionConfigMetaDataParser.parse(reader, propertyReplacer));
                            break;
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static String getElementText(final XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        return propertyReplacer.replaceProperties(reader.getElementText());
    }

    private static XMLStreamException endOfDocument(final Location location) {
        return ServerLogger.ROOT_LOGGER.unexpectedEndOfDocument(location);
    }
}
