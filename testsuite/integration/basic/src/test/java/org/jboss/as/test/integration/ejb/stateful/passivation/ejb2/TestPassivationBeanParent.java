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
import javax.ejb.EJBException;

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
