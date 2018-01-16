/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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



package org.jboss.as.test.integration.ejb.transaction.cmt.fail;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.jboss.as.test.integration.transactions.TestLastResource;
import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class InnerBean {

    @Inject
    private TransactionCheckerSingleton checker;

    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    public void innerMethodXA() {
        try {
            TestXAResource xaResource = new TestXAResource(TestXAResource.TestAction.COMMIT_THROW_XAER_RMFAIL, checker);
            TxTestUtil.enlistTestXAResource(tm.getTransaction(), xaResource);
        } catch (SystemException se) {
            throw new IllegalStateException("Can't get transaction from transaction manager: " + tm);
        }
    }

    public void innerMethod2pcXA() {
        try {
            TestXAResource xaResource1 = new TestXAResource(TestXAResource.TestAction.COMMIT_THROW_XAER_RMFAIL, checker);
            TestXAResource xaResource2 = new TestXAResource(checker);
            TxTestUtil.enlistTestXAResource(tm.getTransaction(), xaResource1);
            TxTestUtil.enlistTestXAResource(tm.getTransaction(), xaResource2);
        } catch (SystemException se) {
            throw new IllegalStateException("Can't get transaction from transaction manager: " + tm);
        }
    }

    public void innerMethodLocal() {
        try {
            TestXAResource xaResource = new TestLastResource(TestXAResource.TestAction.COMMIT_THROW_XAER_RMFAIL, checker);
            TxTestUtil.enlistTestXAResource(tm.getTransaction(), xaResource);
        } catch (SystemException se) {
            throw new IllegalStateException("Can't get transaction from transaction manager: " + tm);
        }
    }
}