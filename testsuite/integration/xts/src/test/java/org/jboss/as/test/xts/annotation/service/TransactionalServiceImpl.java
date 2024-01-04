/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.xts.annotation.service;

import com.arjuna.ats.jta.TransactionManager;
import org.jboss.logging.Logger;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@Stateless
@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class TransactionalServiceImpl implements TransactionalService {

    private static Logger LOG = Logger.getLogger(TransactionalServiceImpl.class);

    @Override
    public boolean isTransactionActive() {
        LOG.debug("TransactionalServiceImpl.isTransactionActive()");

        Transaction transaction = null;

        try {
            transaction = TransactionManager.transactionManager().getTransaction();
        } catch (SystemException e) {
        }

        if (transaction == null) {
            return false;
        }

        try {
            return transaction.getStatus() == Status.STATUS_ACTIVE;
        } catch (SystemException e) {
            return false;
        }
    }

}
