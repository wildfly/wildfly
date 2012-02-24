/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cmp.context;

import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.TransactionEntityMap;
import org.jboss.as.cmp.component.CmpEntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.as.ejb3.context.EntityContextImpl;

/**
 * @author John Bailey
 */
public class CmpEntityBeanContext extends EntityContextImpl {

    private Object persistenceContext;
    private boolean valid;
    private boolean readOnly;

    /**
     * Specifies whether the instance is associated with a transaction and should be synchronized.
     */
    private TransactionEntityMap.TxAssociation txAssociation = TransactionEntityMap.NONE;

    public CmpEntityBeanContext(final EntityBeanComponentInstance componentInstance) {
        super(componentInstance);
    }

    public CmpEntityBeanComponent getComponent() {
        return (CmpEntityBeanComponent) super.getComponent();
    }

    public Object getPersistenceContext() {
        return persistenceContext;
    }

    public void setPersistenceContext(final Object persistenceContext) {
        this.persistenceContext = persistenceContext;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(final boolean valid) {
        this.valid = valid;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Transaction getTransaction() {
        try {
            return getComponent().getTransactionManager().getTransaction();
        } catch (SystemException e) {
            throw CmpMessages.MESSAGES.failedToGetCurrentTransaction(e);
        }
    }

    public TransactionEntityMap.TxAssociation getTxAssociation() {
        return txAssociation;
    }

    public void setTxAssociation(TransactionEntityMap.TxAssociation txAssociation) {
        this.txAssociation = txAssociation;
    }
}
