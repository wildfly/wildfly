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

package org.jboss.as.test.integration.ejb.remove.method;

import java.rmi.RemoteException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author Jaikiran Pai
 */
public class Ejb21ViewDDBean implements SessionBean {

    @Override
    public void setSessionContext(SessionContext sessionContext) throws EJBException, RemoteException {

    }

    public void ejbCreate() {
    }

    @Override
    public void ejbRemove() throws EJBException, RemoteException {
        try {
            final Context ctx = new InitialContext();
            final RemoveMethodInvocationTracker removeMethodInvocationTracker = (RemoveMethodInvocationTracker) ctx.lookup("java:app/remove-method-test/" + RemoveMethodInvocationTrackerBean.class.getSimpleName() + "!" + RemoveMethodInvocationTracker.class.getName());
            removeMethodInvocationTracker.ejbRemoveCallbackInvoked();
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }

    }

    @Override
    public void ejbActivate() throws EJBException, RemoteException {
    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {
    }

    public String test() throws RemoteException {
        return this.getClass().getName();
    }

}
