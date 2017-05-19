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

package org.jboss.as.test.integration.ejb.transaction.exception;

import java.lang.reflect.Constructor;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.logging.Logger;

import javassist.ClassPool;
import javassist.CtClass;

/**
 * Implementation of XAResource for use in tests.
 *
 * @author dsimko@redhat.com
 */
public class TestXAResource implements XAResource {

    private static Logger LOG = Logger.getLogger(TestXAResource.class);
    private static XAException RM_SPECIFIC_EXCEPTION = createDriverSpecificXAException(XAException.XAER_RMERR);

    public static enum CommitOperation {
        NONE, THROW_KNOWN_XA_EXCEPTION, THROW_UNKNOWN_XA_EXCEPTION
    }

    public static enum PrepareOperation {
        NONE, THROW_KNOWN_XA_EXCEPTION, THROW_UNKNOWN_XA_EXCEPTION
    }

    private CommitOperation commitOp = CommitOperation.NONE;
    private PrepareOperation prepareOp = PrepareOperation.NONE;

    public TestXAResource(CommitOperation commitOp) {
        this.commitOp = commitOp;
    }

    public TestXAResource(PrepareOperation prepOp) {
        this.prepareOp = prepOp;
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        LOG.debugf("commit xid:[%s], %s one phase", xid, onePhase ? "with" : "without");

        switch (commitOp) {
        case THROW_KNOWN_XA_EXCEPTION:
            throw new XAException(XAException.XAER_RMERR);
        case THROW_UNKNOWN_XA_EXCEPTION:
            throw RM_SPECIFIC_EXCEPTION;
        case NONE:
        default:
            // do nothing
        }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        LOG.debugf("end xid:[%s], flag: %s", xid, flags);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        LOG.debugf("forget xid:[%s]", xid);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        LOG.debugf("getTransactionTimeout: returning timeout: %s", 0);
        return 0;
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        LOG.debugf("isSameRM returning false to xares: %s", xares);
        return false;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        LOG.debugf("prepare xid: [%s]", xid);
        switch (prepareOp) {
        case THROW_KNOWN_XA_EXCEPTION:
            throw new XAException(XAException.XAER_RMERR);
        case THROW_UNKNOWN_XA_EXCEPTION:
            throw RM_SPECIFIC_EXCEPTION;
        case NONE:
        default:
            return XAResource.XA_OK;
        }
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        LOG.debugf("recover with flags: %s", flag);
        return new Xid[] {};
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        LOG.debugf("rollback xid: [%s]", xid);

    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        LOG.debugf("setTransactionTimeout: setting timeout: %s", seconds);
        return true;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        LOG.debugf("start xid: [%s], flags: %s", xid, flags);
    }

    /**
     * Creates instance of dynamically created XAException class.
     */
    private static XAException createDriverSpecificXAException(int xaErrorCode) {
        try {
            return createInstanceOfDriverSpecificXAException(xaErrorCode, createXATestExceptionClass());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates new instance of given class.
     */
    private static XAException createInstanceOfDriverSpecificXAException(int xaErrorCode, Class<?> clazz) throws Exception {
        Constructor<?> constructor = clazz.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
        return (XAException) constructor.newInstance(xaErrorCode);
    }

    /**
     * Creates new public class named org.jboss.as.test.XATestException.
     */
    private static Class<?> createXATestExceptionClass() throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass evalClass = pool.makeClass("org.jboss.as.test.XATestException", pool.get("javax.transaction.xa.XAException"));
        return evalClass.toClass();
    }

}
