/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.transaction.inflow;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.TransactionContext;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;

/**
 * Implementation of resource adapter for transaction inflow testing.
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
public class TransactionInflowResourceAdapter implements ResourceAdapter {
    private static final Logger log = Logger.getLogger(TransactionInflowResourceAdapter.class);

    private BootstrapContext bootstrapContext;

    static final String MSG = "inflow RAR test message";
    static final String ACTION_COMMIT = "commit";
    static final String ACTION_ROLLBACK = "rollback";

    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        log.tracef("Starting '%s' with context '%s'", TransactionInflowResourceAdapter.class.getSimpleName(), ctx);
        this.bootstrapContext = ctx;
    }

    public void stop() {
        log.tracef("Stopping (do nothing) '%s'", TransactionInflowResourceAdapter.class.getSimpleName());
    }

    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
        Xid xid = TransactionInflowXid.getUniqueXid(42);
        TransactionInflowWork work = new TransactionInflowWork(endpointFactory, MSG);
        TransactionContext txnCtx = new TransactionContext();
        txnCtx.setXid(xid);

        TransactionInflowWorkListener workListener = new TransactionInflowWorkListener();

        try {
            bootstrapContext.getWorkManager().startWork(work, WorkManager.IMMEDIATE, txnCtx, workListener);
        } catch (WorkException e) {
            throw new IllegalStateException("Can't start work " + work + " with txn " + txnCtx);
        }

        // start Work blocks until the execution starts but not until its completion
        int timeout = TimeoutUtil.adjust(10_000); // timeout 10 seconds
        long start = System.currentTimeMillis();
        while(!workListener.isCompleted() && (System.currentTimeMillis() - start < timeout)) {
            Thread.yield(); // active waiting
        }
        if(!workListener.isCompleted()) throw new IllegalStateException("Work " + work + " of xid " + xid + " does not finish.");

        try {
            bootstrapContext.getXATerminator().prepare(xid);

            // depends on value in spec we commit or roll-back
            TransactionInflowRaSpec activationSpec = (TransactionInflowRaSpec) spec;
            if(activationSpec.getAction().equals(ACTION_COMMIT)) {
                bootstrapContext.getXATerminator().commit(xid, false);
            } else if(activationSpec.getAction().equals(ACTION_ROLLBACK)) {
                bootstrapContext.getXATerminator().rollback(xid);
            } else {
                new IllegalStateException("Spec '" + activationSpec + "' defines unknown action");
            }
        } catch (XAException xae) {
            throw new IllegalStateException("Can't process prepare/commit/rollback calls for xid: " + xid, xae);
        }
    }

    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        // nothing to do
    }

    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bootstrapContext == null) ? 0 : bootstrapContext.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TransactionInflowResourceAdapter other = (TransactionInflowResourceAdapter) obj;
        if (bootstrapContext == null) {
            if (other.bootstrapContext != null)
                return false;
        } else if (!bootstrapContext.equals(other.bootstrapContext))
            return false;
        return true;
    }
}
