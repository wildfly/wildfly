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

package org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb3;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxManagerException;
import org.jboss.as.test.integration.ejb.transaction.exception.TimeoutTestXAResource;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.TestBean;
import org.jboss.as.test.integration.transactions.TestXAResource.TestAction;

@LocalBean
@Remote
@Stateless
public class CmtEjb3 implements TestBean {

    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    @Override
    public void throwRuntimeException() {
        throw new RuntimeException();
    }

    @Override
    public void throwExceptionFromTm(TxManagerException txManagerException) throws Exception {
        Transaction txn = tm.getTransaction();
        switch (txManagerException) {
        case HEURISTIC_CAUSED_BY_XA_EXCEPTION:
            txn.enlistResource(new TimeoutTestXAResource(TestAction.NONE));
            txn.enlistResource(new TimeoutTestXAResource(TestAction.COMMIT_THROW_XAER_RMERR));
            break;
        case HEURISTIC_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION:
            txn.enlistResource(new TimeoutTestXAResource(TestAction.NONE));
            txn.enlistResource(new TimeoutTestXAResource(TestAction.COMMIT_THROW_UNKNOWN_XA_EXCEPTION));
            break;
        case ROLLBACK_CAUSED_BY_XA_EXCEPTION:
            txn.enlistResource(new TimeoutTestXAResource(TestAction.NONE));
            txn.enlistResource(new TimeoutTestXAResource(TestAction.PREPARE_THROW_XAER_RMERR));
            break;
        case ROLLBACK_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION:
            txn.enlistResource(new TimeoutTestXAResource(TestAction.NONE));
            txn.enlistResource(new TimeoutTestXAResource(TestAction.PREPARE_THROW_UNKNOWN_XA_EXCEPTION));
            break;
        default:
            throw new IllegalArgumentException("Unknown type " + txManagerException);
        }

    }

}
