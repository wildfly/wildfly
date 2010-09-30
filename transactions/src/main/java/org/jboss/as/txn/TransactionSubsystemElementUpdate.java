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

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;

/**
 * The transaction subsystem update.
 *
 * @author Emanuel Muckenhuber
 */
public class TransactionSubsystemElementUpdate extends AbstractSubsystemUpdate<TransactionsSubsystemElement, Void> {

    private static final long serialVersionUID = 8491562773438385413L;

    private RecoveryEnvironmentElement recoveryEnvironmentElement;
    private CoreEnvironmentElement coreEnvironmentElement;
    private CoordinatorEnvironmentElement coordinatorEnvironmentElement;
    private ObjectStoreEnvironmentElement objectStoreEnvironmentElement;

    protected TransactionSubsystemElementUpdate() {
        super(Namespace.TRANSACTIONS_1_0.getUriString(), true);
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<TransactionsSubsystemElement, ?> getCompensatingUpdate(TransactionsSubsystemElement original) {
        final TransactionSubsystemElementUpdate update = new TransactionSubsystemElementUpdate();
        update.setRecoveryEnvironmentElement(original.getRecoveryEnvironmentElement());
        update.setCoreEnvironmentElement(original.getCoreEnvironmentElement());
        update.setCoordinatorEnvironmentElement(original.getCoordinatorEnvironmentElement());
        update.setObjectStoreEnvironmentElement(original.getObjectStoreEnvironmentElement());
        return update;
    }

    /** {@inheritDoc} */
    public Class<TransactionsSubsystemElement> getModelElementType() {
        return TransactionsSubsystemElement.class;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(TransactionsSubsystemElement element) throws UpdateFailedException {
        if(recoveryEnvironmentElement != null) element.setRecoveryEnvironmentElement(recoveryEnvironmentElement);
        if(coreEnvironmentElement != null) element.setCoreEnvironmentElement(coreEnvironmentElement);
        if(coordinatorEnvironmentElement != null) element.setCoordinatorEnvironmentElement(coordinatorEnvironmentElement);
        if(objectStoreEnvironmentElement != null) element.setObjectStoreEnvironmentElement(objectStoreEnvironmentElement);
    }

    public RecoveryEnvironmentElement getRecoveryEnvironmentElement() {
        return recoveryEnvironmentElement;
    }

    public void setRecoveryEnvironmentElement(RecoveryEnvironmentElement recoveryEnvironmentElement) {
        this.recoveryEnvironmentElement = recoveryEnvironmentElement;
    }

    public CoreEnvironmentElement getCoreEnvironmentElement() {
        return coreEnvironmentElement;
    }

    public void setCoreEnvironmentElement(CoreEnvironmentElement coreEnvironmentElement) {
        this.coreEnvironmentElement = coreEnvironmentElement;
    }

    public CoordinatorEnvironmentElement getCoordinatorEnvironmentElement() {
        return coordinatorEnvironmentElement;
    }

    public void setCoordinatorEnvironmentElement(CoordinatorEnvironmentElement coordinatorEnvironmentElement) {
        this.coordinatorEnvironmentElement = coordinatorEnvironmentElement;
    }

    public ObjectStoreEnvironmentElement getObjectStoreEnvironmentElement() {
        return objectStoreEnvironmentElement;
    }

    public void setObjectStoreEnvironmentElement(ObjectStoreEnvironmentElement objectStoreEnvironmentElement) {
        this.objectStoreEnvironmentElement = objectStoreEnvironmentElement;
    }

}
