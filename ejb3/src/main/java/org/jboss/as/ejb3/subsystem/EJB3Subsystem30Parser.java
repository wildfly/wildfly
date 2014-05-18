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

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;
import java.util.EnumSet;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DATABASE_DATA_STORE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.TIMER_SERVICE;


/**
 */
public class EJB3Subsystem30Parser extends EJB3Subsystem20Parser {

    public static final EJB3Subsystem30Parser INSTANCE = new EJB3Subsystem30Parser();

    protected EJB3Subsystem30Parser() {
    }

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_3_0;
    }

    @Override
    protected void parseDatabaseDataStore(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        String name = null;

        final ModelNode databaseDataStore = new ModelNode();
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
                    DatabaseDataStoreResourceDefinition.DATASOURCE_JNDI_NAME.parseAndSetParameter(value, databaseDataStore, reader);
                    break;
                case DATABASE:
                    DatabaseDataStoreResourceDefinition.DATABASE.parseAndSetParameter(value, databaseDataStore, reader);
                    break;
                case PARTITION:
                    DatabaseDataStoreResourceDefinition.PARTITION.parseAndSetParameter(value, databaseDataStore, reader);
                    break;
                case REFRESH_INTERVAL:
                    DatabaseDataStoreResourceDefinition.REFRESH_INTERVAL.parseAndSetParameter(value, databaseDataStore, reader);
                    break;
                case ALLOW_EXECUTION:
                    DatabaseDataStoreResourceDefinition.ALLOW_EXECUTION.parseAndSetParameter(value, databaseDataStore, reader);
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
        databaseDataStore.get(OP).set(ADD);
        databaseDataStore.get(ADDRESS).set(address);
        operations.add(databaseDataStore);
        requireNoContent(reader);
    }

}
