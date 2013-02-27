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

package org.jboss.as.ejb3.subsystem;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DATABASE_DATA_STORE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DATASOURCE_JNDI_NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.FILE_DATA_STORE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.PATH;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.RELATIVE_TO;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.TIMER_SERVICE;


/**
 */
public class EJB3Subsystem20Parser extends EJB3Subsystem14Parser {

    public static final EJB3Subsystem20Parser INSTANCE = new EJB3Subsystem20Parser();

    protected EJB3Subsystem20Parser() {
    }

    @Override
    protected void readElement(final XMLExtendedStreamReader reader, final EJB3SubsystemXMLElement element, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        switch (element) {
            case TIMER_SERVICE: {
                parseTimerService(reader, operations);
                break;
            }
            default: {
                super.readElement(reader, element, operations, ejb3SubsystemAddOperation);
            }
        }
    }

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_2_0;
    }


    private void parseTimerService(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);
        address.add(SERVICE, TIMER_SERVICE);
        final ModelNode timerServiceAdd = new ModelNode();
        timerServiceAdd.get(OP).set(ADD);
        timerServiceAdd.get(OP_ADDR).set(address);

        final int attCount = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.THREAD_POOL_NAME, EJB3SubsystemXMLAttribute.DEFAULT_DATA_STORE);
        for (int i = 0; i < attCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case THREAD_POOL_NAME: {
                    TimerServiceResourceDefinition.THREAD_POOL_NAME.parseAndSetParameter(value,timerServiceAdd,reader);
                    break;
                }
                case DEFAULT_DATA_STORE: {
                    TimerServiceResourceDefinition.DEFAULT_DATA_STORE.parseAndSetParameter(value,timerServiceAdd,reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        operations.add(timerServiceAdd);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case DATA_STORES: {
                    parseDataStores(reader, operations);
                }
            }
        }
    }

    private void parseDataStores(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case FILE_DATA_STORE: {
                    parseFileDataStore(reader, operations);
                    break;
                }
                case DATABASE_DATA_STORE: {
                    parseDatabaseDataStore(reader, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseFileDataStore(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        String dataStorePath = null;
        String dataStorePathRelativeTo = null;
        String name = null;
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.NAME, EJB3SubsystemXMLAttribute.PATH);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    if (name != null) {
                        throw unexpectedAttribute(reader, i);
                    }
                    name = reader.getAttributeValue(i);
                    break;
                case PATH:
                    if (dataStorePath != null) {
                        throw unexpectedAttribute(reader, i);
                    }
                    dataStorePath = FileDataStoreResourceDefinition.PATH.parse(value, reader).asString();
                    break;
                case RELATIVE_TO:
                    if (dataStorePathRelativeTo != null) {
                        throw unexpectedAttribute(reader, i);
                    }
                    dataStorePathRelativeTo = FileDataStoreResourceDefinition.RELATIVE_TO.parse(value, reader).asString();
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);
        address.add(SERVICE, TIMER_SERVICE);
        address.add(FILE_DATA_STORE, name);
        final ModelNode fileDataStoreAdd = new ModelNode();
        fileDataStoreAdd.get(OP).set(ADD);
        fileDataStoreAdd.get(ADDRESS).set(address);
        fileDataStoreAdd.get(PATH).set(dataStorePath);
        if (dataStorePathRelativeTo != null) {
            fileDataStoreAdd.get(RELATIVE_TO).set(dataStorePathRelativeTo);
        }
        operations.add(fileDataStoreAdd);
        requireNoContent(reader);
    }


    private void parseDatabaseDataStore(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        String name = null;
        String datasourceJndiName = null;
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.NAME, EJB3SubsystemXMLAttribute.DATASOURCE_JNDI_NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    if (name != null) {
                        throw unexpectedAttribute(reader, i);
                    }
                    name = reader.getAttributeValue(i);
                    break;
                case DATASOURCE_JNDI_NAME:
                    if (datasourceJndiName != null) {
                        throw unexpectedAttribute(reader, i);
                    }
                    datasourceJndiName = DatabaseDataStoreResourceDefinition.DATASOURCE_JNDI_NAME.parse(value, reader).asString();
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);
        address.add(SERVICE, TIMER_SERVICE);
        address.add(DATABASE_DATA_STORE, name);
        final ModelNode databaseDataStore = new ModelNode();
        databaseDataStore.get(OP).set(ADD);
        databaseDataStore.get(ADDRESS).set(address);
        databaseDataStore.get(DATASOURCE_JNDI_NAME).set(datasourceJndiName);

        operations.add(databaseDataStore);
        requireNoContent(reader);
    }

}
