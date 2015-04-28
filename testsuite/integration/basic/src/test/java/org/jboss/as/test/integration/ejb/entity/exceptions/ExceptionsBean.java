/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.entity.exceptions;

import javax.ejb.*;
import java.rmi.RemoteException;

public class ExceptionsBean implements EntityBean {

    @Override
    public void setEntityContext(EntityContext ctx) throws EJBException, RemoteException {

    }

    @Override
    public void unsetEntityContext() throws EJBException, RemoteException {

    }

    @Override
    public void ejbActivate() throws EJBException, RemoteException {

    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {

    }

    @Override
    public void ejbLoad() throws EJBException, RemoteException {

    }

    @Override
    public void ejbStore() throws EJBException, RemoteException {

    }

    public String ejbCreate(String key) throws CreateException {
        throw new EJBException("Expected exception");
    }

    public void ejbPostCreate(String key) throws CreateException {

    }

    @Override
    public void ejbRemove() throws RemoveException, EJBException, RemoteException {

    }

    public String ejbFindByPrimaryKey(String key) throws FinderException {
        return key;
    }

    public void throwRuntimeException() {
        throw new EJBException("Expected exception");
    }

    public void test() {

    }
}
