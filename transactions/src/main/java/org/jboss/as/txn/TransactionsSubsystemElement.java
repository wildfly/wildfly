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

import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.services.net.SocketBinding;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.tm.JBossXATerminator;
import org.omg.CORBA.ORB;

import javax.transaction.TransactionManager;
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
        super(new QName("urn:jboss:domain:transactions:1.0", "subsystem"));
    }

    public TransactionsSubsystemElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
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
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
        if(recoveryEnvironmentElement == null) {
            throw missingRequiredElement(reader, Collections.singleton(Element.RECOVERY_ENVIRONMENT.getLocalName()));
        }
        if(coreEnvironmentElement == null) {
            throw missingRequiredElement(reader, Collections.singleton(Element.CORE_ENVIRONMENT.getLocalName()));
        }
        if(coordinatorEnvironmentElement == null) {
            coordinatorEnvironmentElement = new CoordinatorEnvironmentElement();
        }
        if(objectStoreEnvironmentElement == null) {
            objectStoreEnvironmentElement = new ObjectStoreEnvironmentElement();
        }
    }

    public long elementHash() {
        return 42;
    }

    protected Class<TransactionsSubsystemElement> getElementClass() {
        return TransactionsSubsystemElement.class;
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeStartElement(Namespace.TRANSACTIONS_1_0.name(), Element.RECOVERY_ENVIRONMENT.getLocalName());
        recoveryEnvironmentElement.writeContent(streamWriter);
        streamWriter.writeStartElement(Namespace.TRANSACTIONS_1_0.name(), Element.CORE_ENVIRONMENT.getLocalName());
        coreEnvironmentElement.writeContent(streamWriter);
    }

    public void activate(final ServiceActivatorContext context) {
        log.info("Activating Transactions Subsystem");

        final BatchBuilder builder = context.getBatchBuilder();

        // XATerminator has no deps, so just add it in there
        final XATerminatorService xaTerminatorService = new XATerminatorService();
        builder.addService(TxnServices.JBOSS_TXN_XA_TERMINATOR, xaTerminatorService);

        final TransactionManagerService transactionManagerService = new TransactionManagerService(coreEnvironmentElement.getNodeIdentifier(), coreEnvironmentElement.getMaxPorts(), coordinatorEnvironmentElement.isEnableStatistics(), coordinatorEnvironmentElement.getDefaultTimeout(), objectStoreEnvironmentElement.getDirectory());
        final BatchServiceBuilder<TransactionManager> transactionManagerServiceBuilder = builder.addService(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, transactionManagerService);
        transactionManagerServiceBuilder.addOptionalDependency(ServiceName.JBOSS.append("iiop", "orb"), ORB.class, transactionManagerService.getOrbInjector());
        transactionManagerServiceBuilder.addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, transactionManagerService.getXaTerminatorInjector());
        transactionManagerServiceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(recoveryEnvironmentElement.getBindingRef()), SocketBinding.class, transactionManagerService.getRecoveryBindingInjector());
        transactionManagerServiceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(recoveryEnvironmentElement.getStatusBindingRef()), SocketBinding.class, transactionManagerService.getStatusBindingInjector());
        transactionManagerServiceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(coreEnvironmentElement.getBindingRef()), SocketBinding.class, transactionManagerService.getSocketProcessBindingInjector());
        transactionManagerServiceBuilder.setInitialMode(ServiceController.Mode.IMMEDIATE);
    }
}
