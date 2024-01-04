/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.passivation.ejb2;

import java.rmi.RemoteException;
import jakarta.ejb.EJBException;

import org.jboss.logging.Logger;

/**
 * @author Ondrej Chaloupka
 */
public abstract class TestPassivationBeanParent {
    private static final Logger log = Logger.getLogger(TestPassivationBeanParent.class);

    protected String identificator;
    protected boolean beenPassivated = false;
    protected boolean beenActivated = false;

    /**
     * Overriding the ejbPassivate method of SessionBean on child class
     */
    public void ejbPassivate() throws EJBException, RemoteException {
        log.trace(this.toString() + " ejbPassivate [" + this.identificator + "]");
        this.beenPassivated = true;
    }

    /**
     * Overriding the ejbActivate method of SessionBean on child class
     */
    public void ejbActivate() throws EJBException, RemoteException {
        log.trace(this.toString() + " ejbActivate [" + this.identificator + "]");
        this.beenActivated = true;
    }

    /**
     * Overriding the ejbRemove method of SessionBean on child class
     */
    public void ejbRemove() throws EJBException, RemoteException {
        log.trace("Bean [" + this.identificator + "] destroyed");
    }
}
