/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.lazyconnectionmanager.rar;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jesper.pedersen@ironjacamar.org">Jesper Pedersen</a>
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
public class LazyXAResource implements XAResource {

    private static Logger logger = Logger.getLogger(LazyXAResource.class);

    private LazyManagedConnection mc;

    public LazyXAResource(LazyManagedConnection mc) {
        logger.trace("#LazyXAResource");
        this.mc = mc;
    }

    @Override
    public void commit(Xid xid, boolean b) throws XAException {
        logger.trace("#LazyXAResource.commit");
        mc.setEnlisted(false);
    }

    @Override
    public void end(Xid xid, int i) throws XAException {
        logger.trace("#LazyXAResource.end");
    }

    @Override
    public void forget(Xid xid) throws XAException {
        logger.trace("#LazyXAResource.forget");
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        logger.trace("#LazyXAResource.getTransactionTimeout");
        return 0;
    }

    @Override
    public boolean isSameRM(XAResource xaResource) throws XAException {
        logger.trace("#LazyXAResource.isSameRM");
        if (xaResource != null) { return xaResource instanceof LazyXAResource; }
        return false;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        logger.trace("#LazyXAResource.prepare");
        return XAResource.XA_OK;
    }

    @Override
    public Xid[] recover(int i) throws XAException {
        logger.trace("#LazyXAResource.recover");
        return new Xid[0];
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        logger.trace("#LazyXAResource.rollback");
        mc.setEnlisted(false);
    }

    @Override
    public boolean setTransactionTimeout(int i) throws XAException {
        logger.trace("#LazyXAResource.setTransactionTimeout");
        return true;
    }

    @Override
    public void start(Xid xid, int i) throws XAException {
        logger.trace("#LazyXAResource.start");
        mc.setEnlisted(true);
    }
}
