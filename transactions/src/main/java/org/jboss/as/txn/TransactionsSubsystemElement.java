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

import java.util.Collection;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.tm.JBossXATerminator;
import org.omg.CORBA.ORB;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import javax.transaction.TransactionManager;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class TransactionsSubsystemElement extends AbstractSubsystemElement<TransactionsSubsystemElement> {

    private static final long serialVersionUID = 4097067542390229861L;

    public TransactionsSubsystemElement(final Location location) {
        super(location, new QName("urn:jboss:domain:transactions:1.0", "subsystem"));
    }

    public TransactionsSubsystemElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        requireNoContent(reader);
    }

    public long elementHash() {
        return 42;
    }

    protected void appendDifference(final Collection<AbstractModelUpdate<TransactionsSubsystemElement>> target, final TransactionsSubsystemElement other) {
    }

    protected Class<TransactionsSubsystemElement> getElementClass() {
        return TransactionsSubsystemElement.class;
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
    }

    public void activate(final ServiceActivatorContext context) {
        final BatchBuilder builder = context.getBatchBuilder();

        // XATerminator has no deps, so just add it in there
        final XATerminatorService xaTerminatorService = new XATerminatorService();
        builder.addService(TxnServices.JBOSS_TXN_XA_TERMINATOR, xaTerminatorService);

        final TransactionManagerService transactionManagerService = new TransactionManagerService();
        final BatchServiceBuilder<TransactionManager> transactionManagerServiceBuilder = builder.addService(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, transactionManagerService);
        transactionManagerServiceBuilder.addOptionalDependency(ServiceName.JBOSS.append("iiop", "orb"), ORB.class, transactionManagerService.getOrbInjector());
        transactionManagerServiceBuilder.addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, transactionManagerService.getXaTerminatorInjector());
    }

    public Collection<String> getReferencedSocketBindings() {
        return null;
    }
}
