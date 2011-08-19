/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.testsuite.integration.tm.transactionmanager;

import org.jboss.tm.LastResource;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * A LocalResource.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author istudens@redhat.com
 */
public class LocalResource extends Resource implements LastResource {
    /**
     * Whether to fail the commit
     */
    private boolean failLocal = false;

    /**
     * Create a new LocalResource.
     *
     * @param id the id
     */
    public LocalResource(Integer id) {
        super(id);
    }

    public void failLocal() {
        failLocal = true;
    }

    public int prepare(Xid xid) throws XAException {
        XAException e = new XAException("Prepare called on local resource");
        e.errorCode = XAException.XAER_PROTO;
        throw e;
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        State state = getState(xid);
        if (state.resState != ACTIVE && state.resState != ENDED) {
            state.resState = ERROR;
            throw new XAException(XAException.XAER_PROTO);
        }
        if (failLocal) {
            state.resState = ROLLEDBACK;
            state.removed = true;
            throw new XAException(XAException.XA_RBROLLBACK);
        } else {
            state.resState = COMMITTED;
            state.removed = true;
        }
    }

    public boolean isSameRM(XAResource res) {
        return res == this;
    }
}
