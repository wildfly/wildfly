/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.passivation.ejb2;

import java.rmi.RemoteException;
import java.util.Random;

import jakarta.ejb.EJBException;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.SessionBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;

import org.jboss.ejb3.annotation.Cache;
import org.jboss.logging.Logger;

/**
 * @author Ondrej Chaloupka
 */
@Stateful
@Cache("distributable")
@RemoteHome(TestPassivationRemoteHome.class)
public class TestPassivationBean extends TestPassivationBeanParent implements SessionBean {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(TestPassivationBean.class);

    /**
     * Returns the expected result
     */
    public String returnTrueString() {
        return TestPassivationRemote.EXPECTED_RESULT;
    }

    /**
     * Returns whether or not this instance has been passivated
     */
    public boolean hasBeenPassivated() {
        return this.beenPassivated;
    }

    /**
     * Returns whether or not this instance has been activated
     */
    public boolean hasBeenActivated() {
        return this.beenActivated;
    }

    /**
     * "Called" by create() method of home interface.
     */
    public void ejbCreate() {
        Random r = new Random();
        this.identificator = new Integer(r.nextInt(999)).toString();
        log.trace("Bean [" + this.identificator + "] created");
    }

    @Override
    public void setSessionContext(SessionContext arg0) throws EJBException, RemoteException {

    }
}
