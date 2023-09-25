/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.sso.interfaces;

import java.security.Principal;

import jakarta.ejb.CreateException;
import jakarta.ejb.SessionBean;
import jakarta.ejb.SessionContext;
import org.jboss.logging.Logger;


/**
 * A simple session bean for testing declarative security.
 *
 * @author Scott.Stark@jboss.org
 */
public class StatelessSessionBean implements SessionBean {

    private static final long serialVersionUID = -4565135285688543978L;

    static Logger log = Logger.getLogger(StatelessSessionBean.class);

    private SessionContext sessionContext;

    public void ejbCreate() throws CreateException {
        log.debug("ejbCreate() called");
    }

    public void ejbActivate() {
        log.debug("ejbActivate() called");
    }

    public void ejbPassivate() {
        log.debug("ejbPassivate() called");
    }

    public void ejbRemove() {
        log.debug("ejbRemove() called");
    }

    public void setSessionContext(SessionContext context) {
        log.debug("setSessionContext() called");
        sessionContext = context;
    }

    public String echo(String arg) {
        log.debug("echo, arg=" + arg);
        Principal p = sessionContext.getCallerPrincipal();
        log.debug("echo, callerPrincipal=" + p);
        return p.getName();
    }

    public void noop() {
        log.debug("noop");
    }

    public ReturnData getData() {
        ReturnData data = new ReturnData();
        data.data = "TheReturnData";
        return data;
    }

}
