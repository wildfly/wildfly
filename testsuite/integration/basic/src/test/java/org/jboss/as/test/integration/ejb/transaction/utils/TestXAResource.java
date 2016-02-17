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

package org.jboss.as.test.integration.ejb.transaction.utils;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.logging.Logger;

public class TestXAResource implements XAResource {
    private static Logger log = Logger.getLogger(TestXAResource.class);

    private SingletonChecker checker;
    private int transactionTimeout;
    private int prepareReturnValue = XAResource.XA_OK;

    public TestXAResource(SingletonChecker checker) {
        this.checker = checker;
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        log.infof("commit xid:[%s], %s one phase", xid, onePhase ? "with" : "without");
        checker.addCommit();
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        log.infof("end xid:[%s], flag: %s", xid, flags);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        log.infof("forget xid:[%s]", xid);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        log.infof("getTransactionTimeout: returning timeout: %s", transactionTimeout);
        return transactionTimeout;
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        log.infof("isSameRM returning false to xares: %s", xares);
        return false;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        log.infof("prepare xid: [%s]", xid);
        return prepareReturnValue;
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        log.infof("recover with flags: %s", flag);
        return new Xid[]{};
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        log.infof("rollback xid: [%s]", xid);
        checker.addRollback();
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        log.infof("setTransactionTimeout: setting timeout: %s", seconds);
        this.transactionTimeout = seconds;
        return true;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        log.infof("start xid: [%s], flags: %s", xid, flags);
    }

}
