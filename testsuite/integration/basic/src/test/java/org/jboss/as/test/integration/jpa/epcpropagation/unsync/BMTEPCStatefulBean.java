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

package org.jboss.as.test.integration.jpa.epcpropagation.unsync;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.SynchronizationType;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

/**
 * "
 * If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED
 * associated with the JTA transaction and the target component specifies a persistence context of
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
            //Join Transaction to force unsynchronized persistence context to be associated with jta tx
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
            //Join Transaction to force unsynchronized persistence context to be associated with jta tx
            emAllowjoinedunsyncPU.joinTransaction();
            cmtpcStatefulBean.getEmpAllowJoinedUnsyncPersistenceXML(1);
        } finally {
            userTxn.rollback();
        }
    }
}
