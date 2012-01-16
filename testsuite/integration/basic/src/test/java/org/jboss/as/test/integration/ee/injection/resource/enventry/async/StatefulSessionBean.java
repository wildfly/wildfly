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

package org.jboss.as.test.integration.ee.injection.resource.enventry.async;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.SessionSynchronization;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * A StatefulSessionBean.
 * 
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 */
public class StatefulSessionBean implements SessionBean, SessionSynchronization {
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    private InitialContext ic;
    private String afterBeginEntry;
    private String beforeCompletionEntry;
    private String afterCompletionEntry;

    public String getAfterBeginEntry() throws RemoteException {
        return afterBeginEntry;
    }

    public String getBeforeCompletionEntry() throws RemoteException {
        return beforeCompletionEntry;
    }

    public String getAfterCompletionEntry() throws RemoteException {
        return afterCompletionEntry;
    }

    public void ejbCreate() throws EJBException, RemoteException {
    }

    public void ejbActivate() throws EJBException, RemoteException {
    }

    public void ejbPassivate() throws EJBException, RemoteException {
        ic = null;
        afterBeginEntry = null;
        beforeCompletionEntry = null;
        afterCompletionEntry = null;
    }

    public void ejbRemove() throws EJBException, RemoteException {
    }

    public void setSessionContext(SessionContext ctx) throws EJBException, RemoteException {
    }

    public void afterBegin() throws EJBException, RemoteException {
        afterBeginEntry = lookupCompEnv("afterBegin");
    }

    public void afterCompletion(boolean committed) throws EJBException, RemoteException {
        afterCompletionEntry = lookupCompEnv("afterCompletion");
    }

    public void beforeCompletion() throws EJBException, RemoteException {
        beforeCompletionEntry = lookupCompEnv("beforeCompletion");
    }

    private String lookupCompEnv(String name) {
        if (ic == null) {
            try {
                ic = new InitialContext();
            } catch (NamingException e) {
                throw new EJBException("Failed to create initial context.", e);
            }
        }

        try {
            return (String) ic.lookup("java:comp/env/" + name);
        } catch (NamingException e) {
            throw new EJBException("Failed to lookup java:comp/env/" + name, e);
        }
    }
}
