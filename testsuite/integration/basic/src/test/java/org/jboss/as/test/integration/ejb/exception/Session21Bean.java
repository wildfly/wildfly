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

package org.jboss.as.test.integration.ejb.exception;

import org.jboss.as.test.integration.ejb.ejb2.reference.global.Session30RemoteBusiness;
import org.jboss.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.RemoveException;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;

/**
 * @author Jaikiran Pai
 */
public class Session21Bean implements javax.ejb.SessionBean {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(Session21Bean.class);

    private SessionContext sessionContext;

    public void ejbCreate() {

    }

    public void ejbActivate() {

    }

    public void ejbPassivate() {

    }

    public void ejbRemove() {

    }

    public void setSessionContext(SessionContext context) {
        this.sessionContext = context;
    }

    public void invokeOnSelfToThrowCustomRuntimeException() {
        // use a "self" local object so that the subsequent invocation which is expected to
        // throw a runtime exception, triggers a TransactionRolledbackLocalException
        // @see https://issues.jboss.org/browse/AS7-5432
        final Session21Local localObject = (Session21Local) sessionContext.getEJBLocalObject();
        localObject.throwCustomRuntimeException();
    }

    public void throwCustomRuntimeException() {
        throw new CustomRuntimeException();
    }
}
