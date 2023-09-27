/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.log;

import jakarta.ejb.CreateException;
import jakarta.ejb.SessionBean;
import jakarta.ejb.SessionContext;

import org.jboss.logging.Logger;

public class StatefulBean implements SessionBean, StatefulInterface {

    protected SessionContext context;
    protected Logger log = Logger.getLogger( this.getClass() );

    public void ejbCreate() throws CreateException {
        log.info("ejbCreate();");
    }

    public void ejbRemove() {
        log.info("ejbRemove();");
    }

    public void ejbActivate() {
        log.info("ejbActivate();");
    }

    public void ejbPassivate() {
        log.info("ejbPassivate();");
    }

    public void setSessionContext(SessionContext context) {
        this.context = context;
    }
}
