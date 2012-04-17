/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.passivation.ejb2;

import java.rmi.RemoteException;
import java.util.Random;

import javax.ejb.EJBException;
import javax.ejb.RemoteHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import org.jboss.ejb3.annotation.Cache;
import org.jboss.logging.Logger;

/**
 * @author Ondrej Chaloupka
 */
@Stateful
@Cache("passivating")
@RemoteHome(TestPassivationRemoteHome.class)
public class TestPassivationBean implements SessionBean {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(TestPassivationBean.class);

    private String identificator;
    private boolean beenPassivated = false;
    private boolean beenActivated = false;

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


    public void ejbCreate() {
        Random r = new Random();
        this.identificator = new Integer(r.nextInt(999)).toString();
        log.info("Bean [" + this.identificator + "] created");
    }

    @Override
    public void ejbActivate() throws EJBException, RemoteException {
        log.info(this.toString() + " ejbActivate [" + this.identificator + "]");
        this.beenActivated = true;
    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {
        log.info(this.toString() + " ejbPassivate [" + this.identificator + "]");
        this.beenPassivated = true;
    }

    @Override
    public void ejbRemove() throws EJBException, RemoteException {
        log.info("Bean [" + this.identificator + "] destroyed");   
    }

    @Override
    public void setSessionContext(SessionContext arg0) throws EJBException, RemoteException {
        
    }
}
