/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2020, Red Hat, Inc., and individual contributors
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

import org.jboss.logging.Logger;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.Collection;

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

    public static final String XIDS_PERSISTER_FILE_NAME = PersistentTestXAResource.class.getSimpleName();
    private final XidsPersister xidsPersister = new XidsPersister(XIDS_PERSISTER_FILE_NAME);
    private PersistentTestAction persistentTestAction = PersistentTestAction.NONE;

    /**
     * Defines test actions for the persistent {@link XAResource}. This is additional behaviour to
     * what the {@link org.jboss.as.test.integration.transactions.TestXAResource.TestAction} provides.
     * The test actions for the persistent XAResource makes first persistent of the resource and then do some action.
     */
    public enum PersistentTestAction {
        NONE,
        AFTER_PREPARE_THROW_RMFAIL, AFTER_PREPARE_CRASH_JVM,
        AFTER_COMMIT_THROW_RMFAIL, AFTER_COMMIT_CRASH_JVM
    }

    public PersistentTestXAResource() {
        super();
    }

    public PersistentTestXAResource(TransactionCheckerSingleton checker) {
        super(checker);
    }

    public PersistentTestXAResource(TestAction testAction) {
        super(testAction);
    }

    public PersistentTestXAResource(PersistentTestAction persistentTestAction) {
        super();
        this.persistentTestAction = persistentTestAction;
    }

    public PersistentTestXAResource(TestAction testAction, TransactionCheckerSingleton checker) {
        super(testAction, checker);
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        int testXAResourcePrepareResult = super.prepare(xid);
        xidsPersister.writeToDisk(super.getPreparedXids().values());
        log.debugf("Prepared xid [%s] was persisted", xid);

        switch (persistentTestAction) {
            case AFTER_PREPARE_THROW_RMFAIL:
                throw new XAException(XAException.XAER_RMFAIL);
            case AFTER_PREPARE_CRASH_JVM:
                Runtime.getRuntime().halt(0);
            default:
                return testXAResourcePrepareResult;
        }
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        super.commit(xid, onePhase);
        xidsPersister.writeToDisk(super.getPreparedXids().values());

        switch (persistentTestAction) {
            case AFTER_COMMIT_THROW_RMFAIL:
                throw new XAException(XAException.XAER_RMFAIL);
            case AFTER_COMMIT_CRASH_JVM:
                Runtime.getRuntime().halt(0);
            default:
                return;
        }
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        super.rollback(xid);
        xidsPersister.writeToDisk(super.getPreparedXids().values());
    }

    @Override
    public void forget(Xid xid) throws XAException {
        super.forget(xid);
        xidsPersister.writeToDisk(super.getPreparedXids().values());
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        Collection<Xid> recoveredXids = xidsPersister.recoverFromDisk();
        log.debugf("Recover call with flag %d returned %s", recoveredXids);
        return recoveredXids.toArray(new Xid[]{});
    }
}