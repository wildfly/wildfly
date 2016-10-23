/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
