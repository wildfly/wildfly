/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.eararchive;

import jakarta.ejb.SessionContext;
import javax.naming.InitialContext;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public class Test2Bean implements jakarta.ejb.SessionBean {
    private static final long serialVersionUID = -8375644698783606562L;

    public void testAccess() throws Exception {
        InitialContext jndiContext = new InitialContext();

        Test3Business session = (Test3Business) jndiContext.lookup("java:comp/env/ejb/Test3");
        session.testAccess();

        Test3Home home = (Test3Home) jndiContext.lookup("java:comp/env/ejb/Test3Home");
        session = home.create();
        session.testAccess();
    }

    public void ejbCreate() {

    }

    public void ejbActivate() {

    }

    public void ejbPassivate() {

    }

    public void ejbRemove() {

    }

    public void setSessionContext(SessionContext context) {

    }
}
