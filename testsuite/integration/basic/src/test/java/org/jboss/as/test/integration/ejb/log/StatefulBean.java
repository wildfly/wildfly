package org.jboss.as.test.integration.ejb.log;

import org.apache.log4j.Logger;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

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
