/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateful.passivation;

import java.rmi.RemoteException;

import jakarta.ejb.EJBException;
import jakarta.ejb.SessionContext;

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
    public void ejbCreate() throws java.rmi.RemoteException, jakarta.ejb.CreateException {
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
