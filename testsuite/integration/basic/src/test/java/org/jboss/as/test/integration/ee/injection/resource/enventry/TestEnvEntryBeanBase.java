/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.enventry;

import jakarta.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Common base class for "enventry" test EJBs
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
public abstract class TestEnvEntryBeanBase implements TestEnvEntry {

    public int checkJNDI() throws NamingException {
        InitialContext ctx = new InitialContext();
        int rtn = (Integer) ctx.lookup("java:comp/env/maxExceptions");
        if (rtn != (Integer) getSessionContext().lookup("maxExceptions"))
            throw new RuntimeException("Failed to match env lookup");
        return rtn;
    }

    public abstract int getMaxExceptions();

    public abstract int getNumExceptions();

    public abstract int getMinExceptions();

    public abstract SessionContext getSessionContext();
}
