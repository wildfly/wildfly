/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.epcpropagation.unsync;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.persistence.SynchronizationType;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

/**
 * "
 * If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED
 * associated with the Jakarta Transactions transaction and the target component specifies a persistence context of
 * type SynchronizationType.SYNCHRONIZED, the IllegalStateException is
 * thrown by the container.
 * "
 *
 * @author Scott Marlow
 */
@ApplicationScoped
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class BMTEPCStatefulBean {

    @PersistenceContext(synchronization= SynchronizationType.UNSYNCHRONIZED, type = PersistenceContextType.EXTENDED, unitName = "mypc")
    EntityManager em;

    @PersistenceContext(synchronization= SynchronizationType.UNSYNCHRONIZED, type = PersistenceContextType.EXTENDED, unitName = "allowjoinedunsyncPU")
    EntityManager emAllowjoinedunsyncPU;

    @Resource
    SessionContext sessionContext;

    @Inject
    private CMTPCStatefulBean cmtpcStatefulBean;

    public void willThrowError() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        em.isJoinedToTransaction();
        //Begin transaction (before Joining)
        UserTransaction userTxn = sessionContext.getUserTransaction();
        userTxn.begin();
        try {
            cmtpcStatefulBean.getEmp(1);    // show throw IllegalStateException
            throw new RuntimeException("did not get expected IllegalStateException when transaction scoped entity manager used existing unsynchronized xpc");
        } finally {
            userTxn.rollback();
        }
    }

    public void allowjoinedunsync() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        UserTransaction userTxn = sessionContext.getUserTransaction();
        userTxn.begin();
        try {
            //Join Transaction to force unsynchronized persistence context to be associated with Jakarta Transactions tx
            em.joinTransaction();
            cmtpcStatefulBean.getEmpAllowJoinedUnsync(1);
        } finally {
            userTxn.rollback();
        }
    }

    public void allowjoinedunsyncPersistenceXML()  throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        UserTransaction userTxn = sessionContext.getUserTransaction();
        userTxn.begin();
        try {
            //Join Transaction to force unsynchronized persistence context to be associated with Jakarta Transactions tx
            emAllowjoinedunsyncPU.joinTransaction();
            cmtpcStatefulBean.getEmpAllowJoinedUnsyncPersistenceXML(1);
        } finally {
            userTxn.rollback();
        }
    }
}
