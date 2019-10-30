/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb2.stateless.bean;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.SessionContext;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

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
