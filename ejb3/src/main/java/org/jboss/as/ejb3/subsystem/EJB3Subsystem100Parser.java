/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SIMPLE_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DISTRIBUTABLE_CACHE;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser for ejb3:10.0 namespace.
 * TODO Parameterize a single parser class by schema version.  Inheritence is a poor model for versioning.
 */
public class EJB3Subsystem100Parser extends EJB3Subsystem90Parser {

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_10_0;
    }

    protected void parseCaches(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case CACHE: {
                    this.parseCache(reader, operations);
                    break;
                }
                case SIMPLE_CACHE: {
                    this.parseSimpleCache(reader, operations);
                    break;
                }
                case DISTRIBUTABLE_CACHE: {
                    this.parseDistributableCache(reader, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseSimpleCache(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        String name = null;
        ModelNode operation = Util.createAddOperation();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            switch (EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i))) {
                case NAME: {
                    name = value;
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoContent(reader);
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
        }
        final PathAddress address = this.getEJB3SubsystemAddress().append(PathElement.pathElement(SIMPLE_CACHE, name));
        operation.get(OP_ADDR).set(address.toModelNode());
        operations.add(operation);
    }

    private void parseDistributableCache(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        String name = null;
        ModelNode operation = Util.createAddOperation();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            switch (EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i))) {
                case NAME: {
                    name = value;
                    break;
                }
                case BEAN_MANAGEMENT: {
                    AttributeDefinition definition = DistributableCacheFactoryResourceDefinition.Attribute.BEAN_MANAGEMENT.getDefinition();
                    definition.getParser().parseAndSetParameter(definition, value, operation, reader);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoContent(reader);
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
        }
        final PathAddress address = this.getEJB3SubsystemAddress().append(PathElement.pathElement(DISTRIBUTABLE_CACHE, name));
        operation.get(OP_ADDR).set(address.toModelNode());
        operations.add(operation);
    }

    @Override
    protected void parseTimerService(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        PathAddress address = PathAddress.pathAddress(EJB3Extension.SUBSYSTEM_PATH, EJB3SubsystemModel.TIMER_SERVICE_PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.add(operation);

        final int attCount = reader.getAttributeCount();
        for (int i = 0; i < attCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case THREAD_POOL_NAME:
                    TimerServiceResourceDefinition.THREAD_POOL_NAME.parseAndSetParameter(value, operation, reader);
                    break;
                case DEFAULT_DATA_STORE:
                    TimerServiceResourceDefinition.DEFAULT_DATA_STORE.parseAndSetParameter(value, operation, reader);
                    break;
                case DEFAULT_PERSISTENT_TIMER_MANAGEMENT:
                    TimerServiceResourceDefinition.DEFAULT_PERSISTENT_TIMER_MANAGEMENT.parseAndSetParameter(value, operation, reader);
                    break;
                case DEFAULT_TRANSIENT_TIMER_MANAGEMENT:
                    TimerServiceResourceDefinition.DEFAULT_TRANSIENT_TIMER_MANAGEMENT.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case DATA_STORES:
                    parseDataStores(reader, operations);
            }
        }
    }
}
