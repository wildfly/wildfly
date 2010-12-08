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

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class TransactionsSubsystemElement extends AbstractSubsystemElement<TransactionsSubsystemElement> {

    private static final long serialVersionUID = 4097067542390229861L;

    private final RecoveryEnvironmentElement recoveryEnvironmentElement = new RecoveryEnvironmentElement();
    private final CoreEnvironmentElement coreEnvironmentElement = new CoreEnvironmentElement();
    private final CoordinatorEnvironmentElement coordinatorEnvironmentElement = new CoordinatorEnvironmentElement();
    private final ObjectStoreEnvironmentElement objectStoreEnvironmentElement = new ObjectStoreEnvironmentElement();

    public TransactionsSubsystemElement() {
        super(Namespace.TRANSACTIONS_1_0.getUriString());
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

        streamWriter.writeEmptyElement(Element.OBJECT_STORE.getLocalName());
        objectStoreEnvironmentElement.writeContent(streamWriter);

        streamWriter.writeEndElement();
    }

    public RecoveryEnvironmentElement getRecoveryEnvironmentElement() {
        return recoveryEnvironmentElement;
    }

    public CoreEnvironmentElement getCoreEnvironmentElement() {
        return coreEnvironmentElement;
    }

    public CoordinatorEnvironmentElement getCoordinatorEnvironmentElement() {
        return coordinatorEnvironmentElement;
    }

    public void setCoordinatorEnableStatistics(final boolean enable) {
        this.coordinatorEnvironmentElement.setEnableStatistics(enable);
    }

    public void setCoordinatorDefaultTimeout(final int timeout) {
        this.coordinatorEnvironmentElement.setDefaultTimeout(timeout);
    }

    public ObjectStoreEnvironmentElement getObjectStoreEnvironmentElement() {
        return objectStoreEnvironmentElement;
    }

    /** {@inheritDoc} */
    @Override
    protected void getUpdates(List<? super AbstractSubsystemUpdate<TransactionsSubsystemElement, ?>> list) {
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isEmpty() {
        return true;
    }

    @Override
    protected TransactionSubsystemAdd getAdd() {
        final TransactionSubsystemAdd add = new TransactionSubsystemAdd();
        // TODO why not new TransactionSubsystemAdd(this) and do this in c'tor?
        add.setBindingName(coreEnvironmentElement.getBindingRef());
        add.setMaxPorts(coreEnvironmentElement.getMaxPorts());
        add.setNodeIdentifier(coreEnvironmentElement.getNodeIdentifier());
        add.setRecoveryBindingName(recoveryEnvironmentElement.getBindingRef());
        add.setRecoveryStatusBindingName(recoveryEnvironmentElement.getStatusBindingRef());
        add.setCoordinatorEnableStatistics(coordinatorEnvironmentElement.isEnableStatistics());
        add.setCoordinatorDefaultTimeout(coordinatorEnvironmentElement.getDefaultTimeout());
        add.setObjectStorePathRef(objectStoreEnvironmentElement.getRelativeTo());
        add.setObjectStoreDirectory(objectStoreEnvironmentElement.getPath());
        return add;
    }

    @Override
    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
        final ServiceRegistry serviceRegistry = updateContext.getServiceRegistry();
        final ServiceController<?> tmController = serviceRegistry.getService(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER);
        tmController.setMode(ServiceController.Mode.REMOVE);
        final ServiceController<?> xaController = serviceRegistry.getService(TxnServices.JBOSS_TXN_XA_TERMINATOR);
        xaController.setMode(ServiceController.Mode.REMOVE);
    }
}
