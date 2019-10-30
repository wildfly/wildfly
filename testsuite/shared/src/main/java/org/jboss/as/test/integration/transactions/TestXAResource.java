/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.transactions;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.logging.Logger;

/**
 * Test {@link XAResource} class.
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
public class TestXAResource implements XAResource {
    private static Logger log = Logger.getLogger(TestXAResource.class);

    public enum TestAction {
        NONE,
        PREPARE_THROW_XAER_RMERR, PREPARE_THROW_XAER_RMFAIL, PREPARE_THROW_UNKNOWN_XA_EXCEPTION,
        COMMIT_THROW_XAER_RMERR, COMMIT_THROW_XAER_RMFAIL, COMMIT_THROW_UNKNOWN_XA_EXCEPTION
    }

    protected TestAction testAction = TestAction.NONE;
    private TransactionCheckerSingleton checker;
    private int transactionTimeout;

    public TestXAResource(TransactionCheckerSingleton checker) {
        this.checker = checker;
    }

    public TestXAResource(TestAction testAction, TransactionCheckerSingleton checker) {
        this.checker = checker;
        this.testAction = testAction;
    }

    public TestXAResource(TestAction testAction) {
        this.checker = new TransactionCheckerSingleton();
        this.testAction = testAction;
    }

    public TestXAResource() {
        this(TestAction.NONE);
    }


    @Override
    public int prepare(Xid xid) throws XAException {
        log.tracef("prepare xid: [%s]", xid);
        checker.addPrepare();

        switch (testAction) {
            case PREPARE_THROW_XAER_RMERR:
                throw new XAException(XAException.XAER_RMERR);
            case PREPARE_THROW_XAER_RMFAIL:
                throw new XAException(XAException.XAER_RMFAIL);
            case PREPARE_THROW_UNKNOWN_XA_EXCEPTION:
                throw new XAException(null);
            case NONE:
            default:
                return XAResource.XA_OK;
        }
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        log.tracef("commit xid:[%s], %s one phase", xid, onePhase ? "with" : "without");
        checker.addCommit();

        switch (testAction) {
            case COMMIT_THROW_XAER_RMERR:
                throw new XAException(XAException.XAER_RMERR);
            case COMMIT_THROW_XAER_RMFAIL:
                throw new XAException(XAException.XAER_RMFAIL);
            case COMMIT_THROW_UNKNOWN_XA_EXCEPTION:
                throw new XAException(null);
            case NONE:
            default:
                // do nothing
        }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        log.tracef("end xid:[%s], flag: %s", xid, flags);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        log.tracef("forget xid:[%s]", xid);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        log.tracef("getTransactionTimeout: returning timeout: %s", transactionTimeout);
        return transactionTimeout;
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        log.tracef("isSameRM returning false to xares: %s", xares);
        return false;
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        log.tracef("recover with flags: %s", flag);
        return new Xid[]{};
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        log.tracef("rollback xid: [%s]", xid);
        checker.addRollback();
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        log.tracef("setTransactionTimeout: setting timeout: %s", seconds);
        this.transactionTimeout = seconds;
        return true;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        log.tracef("start xid: [%s], flags: %s", xid, flags);
    }
}
