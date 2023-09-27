/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.base;

import java.util.Arrays;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.xts.util.EventLog;
import org.jboss.as.test.xts.util.EventLogEvent;
import org.junit.Assert;

import com.arjuna.mw.wst11.UserBusinessActivity;

/**
 * Shared functionality with XTS test cases.
 */
public abstract class BaseFunctionalTest {

    @ArquillianResource
    private InitialContext iniCtx;

    // ABSTRACT methods used from children classes
    protected abstract EventLog getEventLog();

    // ---- Work with transactions
    public void rollbackIfActive(com.arjuna.mw.wst11.UserTransaction ut) {
        try {
            ut.rollback();
        } catch (Throwable th2) {
            // do nothing, not active
        }
    }

    public void rollbackIfActive(jakarta.transaction.UserTransaction ut) {
        try {
            ut.rollback();
        } catch (Throwable th2) {
            // do nothing, not active
        }
    }

    public void cancelIfActive(UserBusinessActivity uba) {
        try {
            uba.cancel();
        } catch (Throwable e) {
            // do nothing, not active
        }
    }

    protected <T> T lookup(Class<T> beanType, String archiveName) {
        try {
            return beanType.cast(iniCtx.lookup("java:global/" + archiveName + "/" + beanType.getSimpleName() + "!"
                    + beanType.getName()));
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }
    }


    // ---- Test result checking
    protected void assertEventLog(String eventLogName, EventLogEvent... expectedOrder) {
        Assert.assertEquals("Another status order expected for the " + eventLogName, Arrays.asList(expectedOrder), getEventLog().getEventLog(eventLogName));
    }
}
