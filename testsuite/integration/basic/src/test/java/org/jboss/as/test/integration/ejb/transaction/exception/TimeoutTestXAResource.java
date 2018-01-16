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
import javax.transaction.xa.Xid;

import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.logging.Logger;

import javassist.ClassPool;
import javassist.CtClass;

/**
 * Implementation of XAResource for use in tests.
 *
 * @author dsimko@redhat.com
 */
public class TimeoutTestXAResource extends TestXAResource {
    private static final Logger log = Logger.getLogger(TimeoutTestXAResource.class);
    private static XAException RM_SPECIFIC_EXCEPTION = createDriverSpecificXAException(XAException.XAER_RMERR);

    public TimeoutTestXAResource(TestAction testAction) {
        super(testAction);
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        if(super.testAction == TestXAResource.TestAction.COMMIT_THROW_UNKNOWN_XA_EXCEPTION)
            throw RM_SPECIFIC_EXCEPTION;
        else
            super.commit(xid, onePhase);
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        if(super.testAction == TestXAResource.TestAction.PREPARE_THROW_UNKNOWN_XA_EXCEPTION)
            throw RM_SPECIFIC_EXCEPTION;
        else
            return super.prepare(xid);
    }
    /**
     * Creates instance of dynamically created XAException class.
     */
    private static XAException createDriverSpecificXAException(int xaErrorCode) {
        try {
            return createInstanceOfDriverSpecificXAException(xaErrorCode, createXATestExceptionClass());
        } catch (Exception e) {
            log.errorf(e, "Can't create dynamic instance of XAException class", xaErrorCode);
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
