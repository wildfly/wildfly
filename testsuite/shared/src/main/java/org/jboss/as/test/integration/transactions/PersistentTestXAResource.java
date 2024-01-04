/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.transactions;

import java.util.Collection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.logging.Logger;

/**
 * <p>
 * Enhancement of the {@link TestXAResource} which saves prepared {@link Xid} to file system.
 * and delete it from the file on call <code>commit/rollback/forget</code>.
 * </p>
 * <p>
 * Such a {@link Xid} may be reported back to Narayana during periodic recovery when {@link XAResource#recover(int)}
 * is called.
 * </p>
 */
public class PersistentTestXAResource extends TestXAResource implements XAResource {
    private static final Logger log = Logger.getLogger(PersistentTestXAResource.class);
    private XidsPersister xidsPersister = new XidsPersister(PersistentTestXAResource.class.getSimpleName());

    public PersistentTestXAResource() {
        super();
    }

    public PersistentTestXAResource(TransactionCheckerSingleton checker) {
        super(checker);
    }

    public PersistentTestXAResource(TestAction testAction) {
        super(testAction);
    }

    public PersistentTestXAResource(TestAction testAction, TransactionCheckerSingleton checker) {
        super(testAction, checker);
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        int prepareResult = super.prepare(xid);
        xidsPersister.writeToDisk(super.getPreparedXids());
        log.debugf("Prepared xid [%s] was persisted", xid);
        return prepareResult;
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        super.commit(xid, onePhase);
        xidsPersister.writeToDisk(super.getPreparedXids());
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        super.rollback(xid);
        xidsPersister.writeToDisk(super.getPreparedXids());
    }

    @Override
    public void forget(Xid xid) throws XAException {
        super.forget(xid);
        xidsPersister.writeToDisk(super.getPreparedXids());
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        Collection<Xid> recoveredXids = xidsPersister.recoverFromDisk();
        log.debugf("Recover call with flag %d returned %s", recoveredXids);
        return recoveredXids.toArray(new Xid[]{});
    }
}
