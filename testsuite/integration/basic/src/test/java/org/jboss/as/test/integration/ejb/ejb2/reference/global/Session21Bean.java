/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.global;

import jakarta.ejb.SessionContext;
import javax.naming.InitialContext;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public class Session21Bean implements jakarta.ejb.SessionBean {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(Session21Bean.class);

    public String access() {
        return "Session21";
    }

    public String access30() {
        try {
            InitialContext jndiContext = new InitialContext();
            Session30RemoteBusiness session = (Session30RemoteBusiness) jndiContext.lookup("java:comp/env/Session30");
            return session.access();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String globalAccess30() {
        try {
            InitialContext jndiContext = new InitialContext();
            Session30RemoteBusiness session = (Session30RemoteBusiness) jndiContext.lookup("java:global/global-reference-ejb3/GlobalSession30");
            return session.access();
        } catch (Exception e) {
            log.error("Session21Bean.globalAccess30()", e);
            return null;
        }
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
