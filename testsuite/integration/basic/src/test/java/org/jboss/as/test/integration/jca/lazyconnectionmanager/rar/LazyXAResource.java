/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
