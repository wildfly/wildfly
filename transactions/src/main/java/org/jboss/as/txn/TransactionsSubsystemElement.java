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

package org.jboss.as.txn;

import org.jboss.as.model.*;
import org.jboss.as.model.Element;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.util.Collections;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class TransactionsSubsystemElement extends AbstractSubsystemElement<TransactionsSubsystemElement> {

    private static final long serialVersionUID = 4097067542390229861L;
    private static final Logger log = Logger.getLogger("org.jboss.as.transactions");

    private RecoveryEnvironmentElement recoveryEnvironmentElement;
    private CoreEnvironmentElement coreEnvironmentElement;
    private CoordinatorEnvironmentElement coordinatorEnvironmentElement;
    private ObjectStoreEnvironmentElement objectStoreEnvironmentElement;

    public TransactionsSubsystemElement() {
        super(new QName(org.jboss.as.model.Namespace.TRANSACTIONS_1_0.getUriString(), "subsystem").getNamespaceURI());
    }

    public TransactionsSubsystemElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw ParseUtils.unexpectedAttribute(reader, 0);
        }
        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case TRANSACTIONS_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case RECOVERY_ENVIRONMENT: {
                            recoveryEnvironmentElement = new RecoveryEnvironmentElement(reader);
                            break;
                        }
                        case CORE_ENVIRONMENT: {
                            coreEnvironmentElement = new CoreEnvironmentElement(reader);
                            break;
                        }
                        case COORDINATOR_ENVIRONMENT: {
                            coordinatorEnvironmentElement = new CoordinatorEnvironmentElement(reader);
                            break;
                        }
                        case OBJECT_STORE: {
                            objectStoreEnvironmentElement = new ObjectStoreEnvironmentElement(reader);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
        if(recoveryEnvironmentElement == null) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.RECOVERY_ENVIRONMENT.getLocalName()));
        }
        if(coreEnvironmentElement == null) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.CORE_ENVIRONMENT.getLocalName()));
        }
        if(coordinatorEnvironmentElement == null) {
            coordinatorEnvironmentElement = new CoordinatorEnvironmentElement();
        }
        if(objectStoreEnvironmentElement == null) {
            objectStoreEnvironmentElement = new ObjectStoreEnvironmentElement();
        }
    }

    @Override
    private long elementHash() {
        return 42;
    }

    @Override
    protected Class<TransactionsSubsystemElement> getElementClass() {
        return TransactionsSubsystemElement.class;
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        // BES this doesn't work right now; see if we can fix it as it should
        //streamWriter.writeStartElement(Namespace.TRANSACTIONS_1_0.name(), Element.RECOVERY_ENVIRONMENT.getLocalName());
        streamWriter.writeStartElement(Element.RECOVERY_ENVIRONMENT.getLocalName());
        recoveryEnvironmentElement.writeContent(streamWriter);
        // BES this doesn't work right now; see if we can fix it as it should
        //streamWriter.writeStartElement(Namespace.TRANSACTIONS_1_0.name(), Element.CORE_ENVIRONMENT.getLocalName());
        streamWriter.writeStartElement(Element.CORE_ENVIRONMENT.getLocalName());
        coreEnvironmentElement.writeContent(streamWriter);

        streamWriter.writeEndElement();
    }
}
