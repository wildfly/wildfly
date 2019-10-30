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

package org.wildfly.extension.clustering.singleton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parses singleton deployer subsystem configuration from XML.
 * @author Paul Ferraro
 */
public class SingletonXMLReader implements XMLElementReader<List<ModelNode>> {

    @SuppressWarnings("unused")
    private final SingletonSchema schema;

    public SingletonXMLReader(SingletonSchema schema) {
        this.schema = schema;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> result) throws XMLStreamException {
        Map<PathAddress, ModelNode> operations = new LinkedHashMap<>();

        PathAddress address = PathAddress.pathAddress(SingletonResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            XMLElement element = XMLElement.forName(reader);
            switch (element) {
                case SINGLETON_POLICIES: {
                    this.parseSingletonPolicies(reader, address, operations);
                    break;
                }
                default : {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        result.addAll(operations.values());
    }

    private void parseSingletonPolicies(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(address);

        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            XMLAttribute attribute = XMLAttribute.forName(reader, i);
            switch (attribute) {
                case DEFAULT: {
                    readAttribute(reader, i, operation, SingletonResourceDefinition.Attribute.DEFAULT);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            XMLElement element = XMLElement.forName(reader);
            switch (element) {
                case SINGLETON_POLICY: {
                    this.parseSingletonPolicy(reader, address, operations);
                    break;
                }
                default : {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseSingletonPolicy(XMLExtendedStreamReader reader, PathAddress subsystemAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = XMLAttribute.NAME.require(reader);
        PathAddress address = subsystemAddress.append(SingletonPolicyResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            XMLAttribute attribute = XMLAttribute.forName(reader, i);
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case CACHE_CONTAINER: {
                    readAttribute(reader, i, operation, SingletonPolicyResourceDefinition.Attribute.CACHE_CONTAINER);
                    break;
                }
                case CACHE: {
                    readAttribute(reader, i, operation, SingletonPolicyResourceDefinition.Attribute.CACHE);
                    break;
                }
                case QUORUM: {
                    readAttribute(reader, i, operation, SingletonPolicyResourceDefinition.Attribute.QUORUM);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            XMLElement element = XMLElement.forName(reader);
            switch (element) {
                case RANDOM_ELECTION_POLICY: {
                    this.parseRandomElectionPolicy(reader, address, operations);
                    break;
                }
                case SIMPLE_ELECTION_POLICY: {
                    this.parseSimpleElectionPolicy(reader, address, operations);
                    break;
                }
                default : {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseRandomElectionPolicy(XMLExtendedStreamReader reader, PathAddress policyAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = policyAddress.append(RandomElectionPolicyResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        ParseUtils.requireNoAttributes(reader);

        this.parsePreferences(reader, operation);
    }

    private void parseSimpleElectionPolicy(XMLExtendedStreamReader reader, PathAddress policyAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = policyAddress.append(SimpleElectionPolicyResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            XMLAttribute attribute = XMLAttribute.forName(reader, i);
            switch (attribute) {
                case POSITION: {
                    readAttribute(reader, i, operation, SimpleElectionPolicyResourceDefinition.Attribute.POSITION);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        this.parsePreferences(reader, operation);
    }

    @SuppressWarnings("static-method")
    private void parsePreferences(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            XMLElement element = XMLElement.forName(reader);
            switch (element) {
                case NAME_PREFERENCES: {
                    readElement(reader, operation, ElectionPolicyResourceDefinition.Attribute.NAME_PREFERENCES);
                    break;
                }
                case SOCKET_BINDING_PREFERENCES: {
                    readElement(reader, operation, ElectionPolicyResourceDefinition.Attribute.SOCKET_BINDING_PREFERENCES);
                    break;
                }
                default : {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private static void readAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation, Attribute attribute) throws XMLStreamException {
        attribute.getDefinition().getParser().parseAndSetParameter(attribute.getDefinition(), reader.getAttributeValue(index), operation, reader);
    }

    private static void readElement(XMLExtendedStreamReader reader, ModelNode operation, Attribute attribute) throws XMLStreamException {
        attribute.getDefinition().getParser().parseAndSetParameter(attribute.getDefinition(), reader.getElementText(), operation, reader);
    }
}
