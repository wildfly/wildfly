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

package org.jboss.as.test.clustering.cluster.ejb2.stateful.passivation;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.SessionContext;

import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.logging.Logger;

/**
 * @author Ondrej Chaloupka
 */
public abstract class StatefulBeanBase {
    private static Logger log = Logger.getLogger(StatefulBeanBase.class);

    protected int number;
    protected String passivatedBy = "unknown";
    protected String actIfIsNode;
    protected int postActivateCalled = 0;
    protected int prePassivateCalled = 0;

    /**
     * Getting number.
     */
    public int getNumber() {
        return number;
    }

    public String incrementNumber() {
        number++;
        log.trace("Incrementing number: " + Integer.toString(number));
        return NodeNameGetter.getNodeName();
    }

    /**
     * Setting number and returns node name where the method was called.
     */
    public String setNumber(int number) {
        log.trace("Setting number: " + Integer.toString(number));
        this.number = number;
        return NodeNameGetter.getNodeName();
    }

    public String getPassivatedBy() {
        return this.passivatedBy;
    }

    public void setPassivationNode(String nodeName) {
        this.actIfIsNode = nodeName;
    }

    /**
     * Creating method for home interface...
     */
    public void ejbCreate() throws java.rmi.RemoteException, javax.ejb.CreateException {
    }

    /**
     * @Override on SessionBean
     */
    public void ejbActivate() throws EJBException, RemoteException {
        postActivateCalled++;
        log.trace("Activating with number: " + number + " and was passivated by " + getPassivatedBy() + ", postActivate method called " + postActivateCalled + " times");
    }

    /**
     * @Override on SessionBean
     */
    public void ejbPassivate() throws EJBException, RemoteException {
        prePassivateCalled++;
        log.trace("Passivating with number: " + number + " and was passivated by " + getPassivatedBy() + ", prePassivate method called " + prePassivateCalled + " times");

        // when we should act on passivation - we change value of isPassivated variable
        if (NodeNameGetter.getNodeName().equals(actIfIsNode)) {
            passivatedBy = NodeNameGetter.getNodeName();
            log.trace("I'm node " + actIfIsNode + " => changing passivatedBy to " + passivatedBy);
        }
    }

    /**
     * @Override on SessionBean
     */
    public void ejbRemove() throws EJBException, RemoteException {

    }

    /**
     * @Override on SessionBean
     */
    public void setSessionContext(SessionContext arg0) throws EJBException, RemoteException {

    }
}
