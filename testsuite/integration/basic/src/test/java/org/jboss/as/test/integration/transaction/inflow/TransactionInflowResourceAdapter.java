/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.transaction.inflow;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.resource.spi.work.TransactionContext;
import jakarta.resource.spi.work.WorkException;
import jakarta.resource.spi.work.WorkManager;
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
