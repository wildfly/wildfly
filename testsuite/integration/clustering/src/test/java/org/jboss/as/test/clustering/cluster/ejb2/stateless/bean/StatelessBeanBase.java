/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateless.bean;

import java.rmi.RemoteException;

import jakarta.ejb.EJBException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.logging.Logger;

/**
 * @author Ondrej Chaloupka
 */
public abstract class StatelessBeanBase {
    private static final Logger log = Logger.getLogger(StatelessBeanBase.class);

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getNodeName() {
        String nodeName = NodeNameGetter.getNodeName();
        log.trace("StatelessBean.getNodeName() was called on node: " + nodeName);
        return nodeName;
    }

    public void ejbCreate() throws EJBException, RemoteException {
        // creating ejb2 bean
    }

    /**
     * Overrides SessionBean method
     */
    public void ejbActivate() throws EJBException, RemoteException {

    }

    /**
     * Overrides SessionBean method
     */
    public void ejbPassivate() throws EJBException, RemoteException {

    }

    /**
     * Overrides SessionBean method
     */
    public void ejbRemove() throws EJBException, RemoteException {

    }

    /**
     * Overrides SessionBean method
     */
    public void setSessionContext(SessionContext arg0) throws EJBException, RemoteException {

    }
}
