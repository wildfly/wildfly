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

package org.jboss.as.remoting;

import static org.jboss.as.model.ParseUtils.missingRequired;
import static org.jboss.as.model.ParseUtils.readProperty;
import static org.jboss.as.model.ParseUtils.readStringAttributeElement;
import static org.jboss.as.model.ParseUtils.unexpectedAttribute;
import static org.jboss.as.model.ParseUtils.unexpectedElement;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.xnio.SaslQop;
import org.jboss.xnio.SaslStrength;

/**
 * The root element parser for the Remoting subsystem.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class RemotingSubsystemParser implements XMLStreamConstants, XMLElementReader<List<? super AbstractSubsystemUpdate<RemotingSubsystemElement, ?>>> {

    private RemotingSubsystemParser() {
    }

    private static final RemotingSubsystemParser INSTANCE = new RemotingSubsystemParser();

    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static RemotingSubsystemParser getInstance() {
        return INSTANCE;
    }

    /** {@inheritDoc} */
    public void readElement(XMLExtendedStreamReader reader,
            List<? super AbstractSubsystemUpdate<RemotingSubsystemElement, ?>> updates) throws XMLStreamException {
        // Handle attributes
        String threadPoolName = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case THREAD_POOL: {
                        threadPoolName = value;
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (threadPoolName == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.THREAD_POOL));
        }
        // Add the remoting subsystem update
        updates.add(new RemotingSubsystemElementUpdate(threadPoolName));

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case REMOTING_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONNECTOR: {
                            // Add connector updates
                            updates.add(parseConnector(reader));
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    AddConnectorUpdate parseConnector(XMLExtendedStreamReader reader) throws XMLStreamException {

        String name = null;
        String socketBinding = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.SOCKET_BINDING);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    }
                    case SOCKET_BINDING: {
                        socketBinding = value;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
                required.remove(attribute);
            }
        }
        if (! required.isEmpty()) {
            throw ParseUtils.missingRequired(reader, required);
        }
        assert name != null;
        assert socketBinding != null;

        final AddConnectorUpdate update = new AddConnectorUpdate(name, socketBinding);

        // Handle nested elements.
        final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case REMOTING_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (visited.contains(element)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    visited.add(element);
                    switch (element) {
                        case SASL: {
                            update.setSaslElement(parseSaslElement(reader));
                            break;
                        }
                        case PROPERTIES: {
                            parseProperties(reader, update.getProperties());
                            break;
                        }
                        case AUTHENTICATION_PROVIDER: {
                            update.setAuthenticationProvider(readStringAttributeElement(reader, "name"));
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return update;
    }

    SaslElement parseSaslElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final SaslElement saslElement = new SaslElement();

        // No attributes
        final int count = reader.getAttributeCount();
        if (count > 0) {
            throw ParseUtils.unexpectedAttribute(reader, 0);
        }
        // Nested elements
        final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case REMOTING_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (visited.contains(element)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    visited.add(element);
                    switch (element) {
                        case INCLUDE_MECHANISMS: {
                            saslElement.setIncludeMechanisms(ParseUtils.readArrayAttributeElement(reader, "value", String.class));
                            break;
                        }
                        case POLICY: {
                            saslElement.setPolicy(parsePolicyElement(reader));
                            break;
                        }
                        case PROPERTIES: {
                            Map<String, String> map = new HashMap<String, String>();
                            parseProperties(reader, map);
                            saslElement.setProperties(map);
                            break;
                        }
                        case QOP: {
                            saslElement.setQop(ParseUtils.readArrayAttributeElement(reader, "value", SaslQop.class));
                            break;
                        }
                        case REUSE_SESSION: {
                            saslElement.setReuseSession(Boolean.valueOf(ParseUtils.readBooleanAttributeElement(reader, "value")));
                            break;
                        }
                        case SERVER_AUTH: {
                            saslElement.setServerAuth(Boolean.valueOf(ParseUtils.readBooleanAttributeElement(reader, "value")));
                            break;
                        }
                        case STRENGTH: {
                            saslElement.setStrength(ParseUtils.readArrayAttributeElement(reader, "value", SaslStrength.class));
                            break;
                        }
                        default: {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        return saslElement;
    }

    PolicyElement parsePolicyElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        final PolicyElement policy = new PolicyElement();
        if (reader.getAttributeCount() > 0) {
            throw ParseUtils.unexpectedAttribute(reader, 0);
        }
        // Handle nested elements.
        final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case REMOTING_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (visited.contains(element)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    visited.add(element);
                    switch (element) {
                        case FORWARD_SECRECY: {
                            policy.setForwardSecrecy(Boolean.valueOf(ParseUtils.readBooleanAttributeElement(reader, "value")));
                            break;
                        }
                        case NO_ACTIVE: {
                            policy.setNoActive(Boolean.valueOf(ParseUtils.readBooleanAttributeElement(reader, "value")));
                            break;
                        }
                        case NO_ANONYMOUS: {
                            policy.setNoAnonymous(Boolean.valueOf(ParseUtils.readBooleanAttributeElement(reader, "value")));
                            break;
                        }
                        case NO_DICTIONARY: {
                            policy.setNoDictionary(Boolean.valueOf(ParseUtils.readBooleanAttributeElement(reader, "value")));
                            break;
                        }
                        case NO_PLAINTEXT: {
                            policy.setNoPlainText(Boolean.valueOf(ParseUtils.readBooleanAttributeElement(reader, "value")));
                            break;
                        }
                        case PASS_CREDENTIALS: {
                            policy.setPassCredentials(Boolean.valueOf(ParseUtils.readBooleanAttributeElement(reader, "value")));
                            break;
                        }
                        default: {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        return policy;
    }

    void parseProperties(XMLExtendedStreamReader reader, Map<String, String> map) throws XMLStreamException {
        while (reader.nextTag() != END_ELEMENT) {
            reader.require(START_ELEMENT, Namespace.CURRENT.getUriString(), Element.PROPERTY.getLocalName());
            readProperty(reader).addTo(map);
        }
    }



}
