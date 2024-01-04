/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.txn.subsystem;

import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;

import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The {@link org.jboss.staxmapper.XMLElementReader} that handles the Transaction subsystem.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
class TransactionSubsystem20Parser extends TransactionSubsystem14Parser {

    TransactionSubsystem20Parser() {
        super(Namespace.TRANSACTIONS_2_0);
    }

    TransactionSubsystem20Parser(Namespace validNamespace) {
        super(validNamespace);
    }

    protected void parseCoordinatorEnvironmentElement(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STATISTICS_ENABLED:
                    TransactionSubsystemRootResourceDefinition.STATISTICS_ENABLED.parseAndSetParameter(value, operation, reader);
                    break;
                case ENABLE_STATISTICS:
                    TransactionSubsystemRootResourceDefinition.ENABLE_STATISTICS.parseAndSetParameter(value, operation, reader);
                    break;
                case ENABLE_TSM_STATUS:
                    TransactionSubsystemRootResourceDefinition.ENABLE_TSM_STATUS.parseAndSetParameter(value, operation, reader);
                    break;
                case DEFAULT_TIMEOUT:
                    TransactionSubsystemRootResourceDefinition.DEFAULT_TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        // Handle elements
        requireNoContent(reader);

    }

}
