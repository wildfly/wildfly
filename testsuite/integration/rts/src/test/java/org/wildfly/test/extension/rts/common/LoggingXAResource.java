/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.extension.rts.common;

import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public class LoggingXAResource implements XAResource {

    private static final Logger LOG = Logger.getLogger(LoggingXAResource.class);

    private List<String> invocations = new ArrayList<String>();

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        String str = "LoggingXAResource.commit";

        invocations.add(str);

        LOG.trace(str);
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        String str = "LoggingXAResource.end";

        invocations.add(str);

        LOG.trace(str);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        String str = "LoggingXAResource.forget";

        invocations.add(str);

        LOG.trace(str);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        String str = "LoggingXAResource.getTransactionTimeout";

        invocations.add(str);

        LOG.trace(str);

        return 0;
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        String str = "LoggingXAResource.isSameRM";

        invocations.add(str);

        LOG.trace(str);

        return false;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        String str = "LoggingXAResource.prepare";

        invocations.add(str);

        LOG.trace(str);

        return XAResource.XA_OK;
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        String str = "LoggingXAResource.recover";

        invocations.add(str);

        LOG.trace(str);

        return null;
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        String str = "LoggingXAResource.rollback";

        invocations.add(str);

        LOG.trace(str);
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        String str = "LoggingXAResource.setTransactionTimeout";

        invocations.add(str);

        LOG.trace(str);

        return false;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        String str = "LoggingXAResource.start";

        invocations.add(str);

        LOG.trace(str);
    }

    public List<String> getInvocations() {
        return invocations;
    }

    public void resetInvocations() {
        invocations.clear();
    }

}
