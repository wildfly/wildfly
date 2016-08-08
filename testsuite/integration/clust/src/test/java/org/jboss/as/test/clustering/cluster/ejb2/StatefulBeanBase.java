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

package org.jboss.as.test.clustering.cluster.ejb2;

import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.SessionContext;
import java.rmi.RemoteException;

/**
 * @author Jan Martiska
 */
public abstract class StatefulBeanBase {
    private static Logger log = Logger.getLogger(StatefulBeanBase.class);
    
    protected int number;

    /**
     * Getting number.
     */
    public int getNumber() {
        log.info(NodeNameGetter.getNodeName() + " getting number: " + Integer.toString(number));
        return number;
    }

    public String incrementNumber() {
        number++;
        log.info(NodeNameGetter.getNodeName() + " incrementing number: " + Integer.toString(number));
        return NodeNameGetter.getNodeName();
    }

    /** 
     * Creating method for home interface...
     */
    public void ejbCreate() throws RemoteException, javax.ejb.CreateException {
    }
    
    /** 
     * @Override on SessionBean
     */
    public void ejbActivate() throws EJBException, RemoteException {
    }

    /** 
     * @Override on SessionBean
     */
    public void ejbPassivate() throws EJBException, RemoteException {
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
