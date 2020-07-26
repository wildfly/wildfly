/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat Middleware LLC, and individual contributors
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
 *
 */
package org.jboss.as.txn.subsystem;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.controller.parsing.ParseUtils.*;

/**
 * The {@link org.jboss.staxmapper.XMLElementReader} that handles the version 6.0 of Transaction subsystem xml.
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
class TransactionSubsystem60Parser extends TransactionSubsystem50Parser {

    TransactionSubsystem60Parser() {
        super(Namespace.TRANSACTIONS_6_0);
    }

    TransactionSubsystem60Parser(Namespace namespace) {
        super(namespace);
    }

    @Override
    protected void readElement(final XMLExtendedStreamReader reader, final Element element, final List<ModelNode> operations, final ModelNode subsystemOperation, final ModelNode logStoreOperation) throws XMLStreamException {
        switch (element) {
            case RECOVERY_ENVIRONMENT: {
                parseRecoveryEnvironmentElement(reader, subsystemOperation);
                break;
            }
            case CORE_ENVIRONMENT: {
                parseCoreEnvironmentElement(reader, subsystemOperation);
                break;
            }
            case COORDINATOR_ENVIRONMENT: {
                parseCoordinatorEnvironmentElement(reader, subsystemOperation);
                break;
            }
            case OBJECT_STORE: {
                parseObjectStoreEnvironmentElementAndEnrichOperation(reader, subsystemOperation);
                break;
            }
            case JTS: {
                parseJts(reader, subsystemOperation);
                break;
            }
            case USE_JOURNAL_STORE: {
                if (choiceObjectStoreEncountered) {
                    throw unexpectedElement(reader);
                }
                choiceObjectStoreEncountered = true;

                parseUseJournalstore(reader, logStoreOperation, subsystemOperation);
                subsystemOperation.get(CommonAttributes.USE_JOURNAL_STORE).set(true);
                break;
            }
            case JDBC_STORE: {
                if (choiceObjectStoreEncountered) {
                    throw unexpectedElement(reader);
                }
                choiceObjectStoreEncountered = true;

                parseJdbcStoreElementAndEnrichOperation(reader, logStoreOperation, subsystemOperation);
                subsystemOperation.get(CommonAttributes.USE_JDBC_STORE).set(true);
                break;
            }
            case CM_RESOURCES:
                parseCMs(reader, operations);
                break;
            case CLIENT:
                parseClient(reader, subsystemOperation);
                break;
            default: {
                throw unexpectedElement(reader);
            }
        }
    }

    protected void parseClient(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {

        Set<Attribute> required = EnumSet.of(Attribute.STALE_TRANSACTION_TIME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case STALE_TRANSACTION_TIME:
                    TransactionSubsystemRootResourceDefinition.STALE_TRANSACTION_TIME.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        // Handle elements
        requireNoContent(reader);

    }
}
