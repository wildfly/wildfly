/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.entity.cmp.cascadedelete;

import java.rmi.RemoteException;
import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

/**
 * A CustomerBean.
 *
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 * @version $Revision: 82920 $
 */
public abstract class AccountBean implements EntityBean
{
   public Long ejbCreate(Long id, String name) throws CreateException
   {
      setId(id);
      setName(name);
      return null;
   }

   public void ejbPostCreate(Long id, String name) throws CreateException
   {
   }

   public abstract Long getId();
   public abstract void setId(Long id);

   public abstract String getName();
   public abstract void setName(String name);

   public abstract CustomerLocal getCustomer();
   public abstract void setCustomer(CustomerLocal customer);

   public abstract AccountLocal getParentAccount();
   public abstract void setParentAccount(AccountLocal account);

   public abstract Collection getChildAccounts();
   public abstract void setChildAccounts(Collection accounts);

   public abstract AccountLocal getParentAccount2();
   public abstract void setParentAccount2(AccountLocal account);

   public abstract Collection getChildAccounts2();
   public abstract void setChildAccounts2(Collection accounts);

   public void setEntityContext(EntityContext arg0) throws EJBException, RemoteException
   {
   }

   public void unsetEntityContext() throws EJBException, RemoteException
   {
   }

   public void ejbRemove() throws RemoveException, EJBException, RemoteException
   {
   }

   public void ejbActivate() throws EJBException, RemoteException
   {
   }

   public void ejbPassivate() throws EJBException, RemoteException
   {
   }

   public void ejbLoad() throws EJBException, RemoteException
   {
   }

   public void ejbStore() throws EJBException, RemoteException
   {
   }
}
